package joshie.harvestmoon.player;

import static joshie.harvestmoon.HarvestMoon.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import joshie.harvestmoon.entities.EntityNPC;
import joshie.harvestmoon.entities.NPC;
import joshie.harvestmoon.util.IData;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class RelationStats implements IData {
    private HashMap<Relatable, Short> relations = new HashMap();
    private HashSet<Relatable> talkedTo = new HashSet();
    private HashSet<Relatable> marriedTo = new HashSet();

    public PlayerDataServer master;

    public RelationStats(PlayerDataServer master) {
        this.master = master;
    }

    public void newDay() {
        talkedTo = new HashSet();
    }

    public Relatable getRelatable(Object object) {        
        if (object instanceof EntityNPC) {
            return new Relatable(((EntityNPC) object).getNPC());
        } else if (object instanceof EntityLivingBase) {
            return new Relatable(((EntityLivingBase) object).getPersistentID());
        } else if (object instanceof NPC) {
            return new Relatable((NPC) object);
        } else if (object instanceof UUID) {
            return new Relatable((UUID) object);
        } else if (object instanceof Relatable) {
            return (Relatable) object;
        } else return null;
    }

    /** Accepts EntityLivingBase, EntityNPC, NPC or UUID
     * Set this entity as having been talked to today **/
    public void setTalkedTo(Object object) {
        Relatable relate = getRelatable(object);
        if (!talkedTo.contains(relate)) {
            affectRelationship(relate, 100);
            talkedTo.add(relate);
            handler.getServer().markDirty();
        }
    }

    /** Accepts EntityLivingBase, EntityNPC, NPC or UUID, 
     * Affect this entities relationship with the player **/
    public boolean affectRelationship(Object object, int amount) {        
        Relatable relate = getRelatable(object);
        int relation = getRelationship(relate) + amount;
        relations.put(relate, (short) relation);
        handler.getServer().markDirty();
        relate.sendPacket(master.getAndCreatePlayer(), relation, true);
        return true;
    }

    /** Accepts EntityLivingBase, EntityNPC, NPC or UUID
     * Returns the current relationship value of this entity **/
    public int getRelationship(Object object) {
        Relatable relate = getRelatable(object);
        Short ret = relations.get(relate);
        return ret == null ? Short.MIN_VALUE : ret;
    }

    /** Accepts EntityLivingBase, EntityNPC, NPC or UUID
     * remove the relationships for this entity, mostly called on death **/
    public boolean removeRelations(Object object) {
        Relatable relate = getRelatable(object);
        relations.remove(relate);
        handler.getServer().markDirty();
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NBTTagList relationships = nbt.getTagList("Relationships", 10);
        for (int i = 0; i < relationships.tagCount(); i++) {
            NBTTagCompound tag = relationships.getCompoundTagAt(i);
            Relatable relatable = new Relatable();
            relatable.readFromNBT(tag);
            short value = tag.getShort("Value");
            relations.put(relatable, value);
        }

        NBTTagList talked = nbt.getTagList("TalkedTo", 10);
        for (int i = 0; i < talked.tagCount(); i++) {
            NBTTagCompound tag = talked.getCompoundTagAt(i);
            Relatable relatable = new Relatable();
            relatable.readFromNBT(tag);
            talkedTo.add(relatable);
        }

        NBTTagList married = nbt.getTagList("MarriedTo", 10);
        for (int i = 0; i < married.tagCount(); i++) {
            NBTTagCompound tag = married.getCompoundTagAt(i);
            Relatable relatable = new Relatable();
            relatable.readFromNBT(tag);
            marriedTo.add(relatable);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList relationships = new NBTTagList();
        for (Map.Entry<Relatable, Short> entry : relations.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            entry.getKey().writeToNBT(tag);
            tag.setShort("Value", entry.getValue());
            relationships.appendTag(tag);
        }

        nbt.setTag("Relationships", relationships);

        //////////////////////////////////////////////////////////////////////////

        NBTTagList talked = new NBTTagList();
        for (Relatable r : talkedTo) {
            NBTTagCompound tag = new NBTTagCompound();
            r.writeToNBT(tag);
            talked.appendTag(tag);
        }

        nbt.setTag("TalkedTo", talked);

        //////////////////////////////////////////////////////////////////////////

        NBTTagList married = new NBTTagList();
        for (Relatable r : marriedTo) {
            NBTTagCompound tag = new NBTTagCompound();
            r.writeToNBT(tag);
            married.appendTag(tag);
        }

        nbt.setTag("MarriedTo", married);
    }
}
