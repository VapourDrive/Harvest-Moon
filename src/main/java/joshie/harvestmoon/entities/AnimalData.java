package joshie.harvestmoon.entities;

import static joshie.harvestmoon.HarvestMoon.handler;
import static joshie.harvestmoon.entities.AnimalData.AnimalType.CAT;
import static joshie.harvestmoon.entities.AnimalData.AnimalType.CHICKEN;
import static joshie.harvestmoon.entities.AnimalData.AnimalType.COW;
import static joshie.harvestmoon.entities.AnimalData.AnimalType.DOG;
import static joshie.harvestmoon.entities.AnimalData.AnimalType.HORSE;
import static joshie.harvestmoon.entities.AnimalData.AnimalType.OTHER;
import static joshie.harvestmoon.entities.AnimalData.AnimalType.PIG;
import static joshie.harvestmoon.entities.AnimalData.AnimalType.SHEEP;
import static joshie.harvestmoon.helpers.SizeableHelper.getSizeable;
import static joshie.harvestmoon.network.PacketHandler.sendToEveryone;
import static joshie.lib.helpers.ItemHelper.spawnByEntity;

import java.util.Random;
import java.util.UUID;

import joshie.harvestmoon.calendar.CalendarServer;
import joshie.harvestmoon.helpers.RelationsHelper;
import joshie.harvestmoon.lib.SizeableMeta;
import joshie.harvestmoon.lib.SizeableMeta.Size;
import joshie.harvestmoon.network.PacketSyncCanProduce;
import joshie.harvestmoon.util.IData;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class AnimalData implements IData {
    private static final Random rand = new Random();

    //Enum of animal types
    public static enum AnimalType {
        COW(12 * (CalendarServer.DAYS_PER_SEASON * 4), 20 * (CalendarServer.DAYS_PER_SEASON * 4), 1), SHEEP(8 * (CalendarServer.DAYS_PER_SEASON * 4), 12 * (CalendarServer.DAYS_PER_SEASON * 4), 7), CHICKEN(3 * (CalendarServer.DAYS_PER_SEASON * 4), 10 * (CalendarServer.DAYS_PER_SEASON * 4), 1), HORSE(20 * (CalendarServer.DAYS_PER_SEASON * 4), 30 * (CalendarServer.DAYS_PER_SEASON * 4), 0), PIG(6 * (CalendarServer.DAYS_PER_SEASON * 4), 10 * (CalendarServer.DAYS_PER_SEASON * 4), 0), CAT(10 * (CalendarServer.DAYS_PER_SEASON * 4), 20 * (CalendarServer.DAYS_PER_SEASON * 4), 0), DOG(9 * (CalendarServer.DAYS_PER_SEASON * 4), 16 * (CalendarServer.DAYS_PER_SEASON * 4), 0), OTHER(5 * (CalendarServer.DAYS_PER_SEASON * 4), 10 * (CalendarServer.DAYS_PER_SEASON * 4), 0);

        private final int min;
        private final int max;
        private final int days;

        private AnimalType(int min, int max, int days) {
            this.min = min;
            this.max = max;
            this.days = days;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public int getDays() {
            return days;
        }
    }

    private EntityAnimal animal;
    private EntityPlayerMP owner;

    private UUID a_uuid;
    private UUID o_uuid;
    private int dimension; //The dimension this animal was last in

    private AnimalType type;
    private int currentLifespan = 0; //How many days this animal has lived for
    private int healthiness = 127; //How healthy this animal is, full byte range
    private int cleanliness = 0; //How clean this animal is, full byte range
    private int daysNotFed; //How many subsequent days that this animal has not been fed

    private boolean sickCheck; //Whether to check if the animal is sick
    private boolean isSick; //Whether the animal is sick or not

    //Product based stuff
    private int daysPassed; //How many days have passed so far
    private int maxProductsPerDay = 1; //The maximum number of products this animal can produce a day
    private int numProductsProduced = 0; //The number of products this animal has produced today (resets each day)
    private boolean thrown; //Whether this animal has been thrown or not today, only affects chickens
    private boolean treated; //Whether this animal has had it's treat for today

    public AnimalData() {}

    public AnimalData(EntityAnimal animal) {
        this.animal = animal;
        this.a_uuid = animal.getPersistentID();
        if (animal instanceof EntityChicken) {
            this.type = CHICKEN;
        } else if (animal instanceof EntitySheep) {
            this.type = SHEEP;
        } else if (animal instanceof EntityCow) {
            this.type = COW;
        } else if (animal instanceof EntityPig) {
            this.type = PIG;
        } else if (animal instanceof EntityHorse) {
            this.type = HORSE;
        } else if (animal instanceof EntityOcelot) {
            this.type = CAT;
        } else if (animal instanceof EntityWolf) {
            this.type = DOG;
        } else {
            this.type = OTHER;
        }
    }

    /** May return null **/
    public EntityAnimal getAndCreateAnimal() {
        if (animal == null) {
            animal = (EntityAnimal) joshie.lib.helpers.EntityHelper.getAnimalFromUUID(dimension, a_uuid);
        }

        return animal;
    }

    /** May return null **/
    public EntityPlayerMP getAndCreateOwner() {
        if (o_uuid != null) {
            if (owner == null) {
                owner = joshie.lib.helpers.EntityHelper.getPlayerFromUUID(o_uuid);
            }

            return owner;
        } else return null;
    }

    private int getDeathChance(EntityAnimal entity) {
        //If the animal has not been fed, give it a fix changed of dying
        if (daysNotFed > 0) {
            return Math.max(1, 45 - daysNotFed * 3);
        }

        //Gets the adjusted relationship, 0-65k
        int relationship = RelationsHelper.getRelationshipValue(entity, getOwner());
        double chance = (relationship / (double) RelationsHelper.ADJUSTED_MAX) * 200;
        chance += healthiness;
        if (chance <= 1) {
            chance = 1D;
        }

        return (int) chance;
    }

    public boolean newDay() {
        EntityAnimal entity = getAndCreateAnimal();
        if (entity != null) {
            //Stage 1, Check if the Animal is going to die
            if (currentLifespan > type.getMax()) return false;
            if (currentLifespan > type.getMin() || isSick) {
                if (rand.nextInt(getDeathChance(entity)) == 0) {
                    return false;
                }
            }

            //Stage 1.5 Chance for animal to get sick if healthiness below 100
            if (!isSick) {
                if (healthiness < 100) {
                    if (rand.nextInt(Math.max(1, healthiness)) == 0) {
                        sickCheck = true;
                        isSick = true;
                    }
                }
            }

            //Stage 2, Do the basic increasing and resetting of counters
            thrown = false;
            treated = false;
            currentLifespan++;
            daysNotFed++;
            daysPassed++;
            healthiness -= daysNotFed;
            if (cleanliness < 0) {
                healthiness += cleanliness;
            }

            if (sickCheck) {
                if (isSick) {
                    animal.addPotionEffect(new PotionEffect(Potion.confusion.id, 1000000, 0));
                    animal.addPotionEffect(new PotionEffect(Potion.blindness.id, 1000000, 0));
                    animal.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 1000000, 0));
                } else {
                    animal.removePotionEffect(Potion.confusion.id);
                    animal.removePotionEffect(Potion.blindness.id);
                    animal.removePotionEffect(Potion.moveSlowdown.id);

                    sickCheck = false;
                }
            }

            if (cleanliness > 0) {
                cleanliness = 0;
            } else if (cleanliness <= 0) {
                cleanliness--;
            }

            //Stage 3, Reset the products produced per day
            if (type.getDays() > 0) {
                if (daysPassed >= type.getDays()) {
                    daysPassed = 0;
                    numProductsProduced = 0;

                    //Stage 4, if the animal is a sheep, make it eat grass
                    if (type == SHEEP) {
                        if (entity != null) {
                            entity.eatGrassBonus();
                        }
                    } else if (type == CHICKEN) { //Or if it's a chicken, make it lay an egg
                        EntityPlayer player = handler.getServer().getAnimalTracker().getOwner(entity);
                        if (entity != null && player != null) {
                            ItemStack egg = getSizeable(player, entity, SizeableMeta.EGG, Size.LARGE);
                            entity.playSound("mob.chicken.plop", 1.0F, (entity.worldObj.rand.nextFloat() - entity.worldObj.rand.nextFloat()) * 0.2F + 1.0F);
                            spawnByEntity(entity, egg);
                        }
                    }
                }

                //Sync Whether this animal can produce, before we increase daysnotfed
                sendToEveryone(new PacketSyncCanProduce(entity.getEntityId(), false, canProduce()));
            }

            return true;
        } else return false;
    }

    //Returns this owner of this animal, can be null
    public EntityPlayer getOwner() {
        EntityAnimal animal = getAndCreateAnimal();
        EntityPlayerMP owner = getAndCreateOwner();
        if (owner != null) {
            if (animal.worldObj.provider.dimensionId == owner.worldObj.provider.dimensionId) {
                if (animal.getDistanceToEntity(owner) <= 128) {
                    return owner;
                } else return null;
            } else return null;
        }

        return animal.worldObj.getClosestPlayerToEntity(animal, 128);
    }

    //Sets the owner of this animal
    public void setOwner(EntityPlayerMP player) {
        this.owner = player;
        this.o_uuid = player.getPersistentID();
    }

    public int getDimension() {
        if (animal != null) {
            return animal.worldObj.provider.dimensionId;
        } else return dimension;
    }

    //Animals can produce products, if they are healthy, have been fed, and aren't over their daily limit
    public boolean canProduce() {
        return healthiness > 0 && daysNotFed <= 0 && numProductsProduced < maxProductsPerDay;
    }

    //Increase the amount of products this animal has produced for the day
    public void setProduced() {
        numProductsProduced++;
        //Increase the amount produced, then resync the data with the client
        EntityAnimal animal = getAndCreateAnimal();
        sendToEveryone(new PacketSyncCanProduce(animal.getEntityId(), false, canProduce()));
    }

    public boolean setCleaned() {
        if (cleanliness < Byte.MAX_VALUE) {
            cleanliness += 10;
            return cleanliness >= Byte.MAX_VALUE;
        } else return false;
    }

    public boolean setThrown() {
        if (!thrown) {
            thrown = true;
            return true;
        } else return false;
    }

    //Sets this animal as having been fed, if it's already been fed, this will return false
    public boolean setFed() {
        if (daysNotFed >= 0) {
            daysNotFed = -1;
            return true;
        } else return false;
    }

    //If the anima hasn't been treated yet today, and it's the right treat, then reward some points
    public void treat(ItemStack stack, EntityPlayer player) {
        if (!treated) {
            int dmg = stack.getItemDamage();
            AnimalType type = dmg < AnimalType.values().length ? AnimalType.values()[dmg] : OTHER;
            if (type == this.type) {
                treated = true;
                handler.getServer().getPlayerData(player).affectRelationship(getAndCreateAnimal(), 1000);
            }
        }
    }

    //Returns true if the animal was healed
    public boolean heal() {
        if (healthiness < 27) {
            healthiness += 100;
            if (healthiness >= 0) {
                isSick = false;
            }

            return true;
        } else return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("Owner-UUIDMost")) {
            o_uuid = new UUID(nbt.getLong("Owner-UUIDMost"), nbt.getLong("Owner-UUIDLeast"));
        }

        a_uuid = new UUID(nbt.getLong("UUIDMost"), nbt.getLong("UUIDLeast"));
        type = AnimalType.values()[nbt.getByte("AnimalType")];
        currentLifespan = nbt.getShort("CurrentLifespan");
        healthiness = nbt.getByte("Healthiness");
        cleanliness = nbt.getByte("Cleanliness");
        daysNotFed = nbt.getByte("DaysNotFed");
        daysPassed = nbt.getByte("DaysPassed");
        treated = nbt.getBoolean("Treated");
        sickCheck = nbt.getBoolean("CheckIfSick");
        isSick = nbt.getBoolean("IsSick");
        dimension = nbt.getInteger("Dimension");
        if (type == CHICKEN) thrown = nbt.getBoolean("Thrown");
        if (type.days > 0) {
            maxProductsPerDay = nbt.getByte("NumProducts");
            numProductsProduced = nbt.getByte("ProductsProduced");
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        if (o_uuid != null) {
            nbt.setLong("Owner-UUIDMost", o_uuid.getMostSignificantBits());
            nbt.setLong("Owner-UUIDLeast", o_uuid.getLeastSignificantBits());
        }

        nbt.setByte("AnimalType", (byte) type.ordinal());
        nbt.setShort("CurrentLifespan", (short) currentLifespan);
        nbt.setByte("Healthiness", (byte) healthiness);
        nbt.setByte("Cleanliness", (byte) cleanliness);
        nbt.setByte("DaysNotFed", (byte) daysNotFed);
        nbt.setByte("DaysPassed", (byte) daysPassed);
        nbt.setBoolean("Treated", treated);
        nbt.setBoolean("IsSick", isSick);
        nbt.setBoolean("CheckIfSick", sickCheck);
        if (animal != null) {
            nbt.setInteger("Dimension", animal.worldObj.provider.dimensionId);
        }

        if (type == CHICKEN) nbt.setBoolean("Thrown", thrown);
        if (type.days > 0) {
            nbt.setByte("NumProducts", (byte) maxProductsPerDay);
            nbt.setByte("ProductsProduced", (byte) numProductsProduced);
        }
    }
}
