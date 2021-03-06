package joshie.harvestmoon.crops;

import static joshie.harvestmoon.crops.CropData.WitherType.NONE;
import static joshie.harvestmoon.helpers.CropHelper.getCropFromOrdinal;
import io.netty.buffer.ByteBuf;

import java.util.Random;

import joshie.harvestmoon.init.HMItems;
import joshie.harvestmoon.util.IData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class CropData implements IData {
    private static final Random rand = new Random();

    public static enum WitherType {
        SEED, GROWING, GROWN, NONE;
    }

    private Crop crop; //The Crop Type of this plant
    private int stage; //The stage it is currently at
    private int quality; //The quality of this crop
    private boolean isGiantCrop; //Whether this crop is giant or not
    private boolean isFertilized; //Whether this crop is currently fertilized
    private int daysWithoutWater; //The number of days this crop has gone without water

    public CropData() {}

    public CropData(Crop crop, int quality) {
        this.crop = crop;
        this.quality = quality;
        this.stage = 1;
    }

    public WitherType newDay() {
        //Stage 1, Check how long the plant has been without water, If it's more than 2 days kill it, decrease it's quality if it's not been watered as well
        if (daysWithoutWater > 2) {
            return crop.getWitherType(stage);
        } else { //Stage 2: Now that we know, it has been watered, Update it's stage
            if (stage < crop.getStages()) {
                stage++;
            }

            //Stage 3, If it's been fertilized, increase it's quality
            if (isFertilized && quality < 100) {
                quality++;
                //Stage 4, Random chance of a giant crop being born
                if (rand.nextInt(255) == 0) {
                    isGiantCrop = true;
                }
            }
        }

        //Stage 5, Reset the giant if conditions are invalid
        if (!isFertilized || daysWithoutWater > 0) {
            isGiantCrop = false;
        }

        //Stage 6, Reset the water counter and fertilising
        daysWithoutWater++;
        isFertilized = false;

        return NONE;
    }

    public String getName() {
        return crop.getUnlocalizedName();
    }

    public boolean isGiant() {
        return isGiantCrop;
    }

    public int getStage() {
        return stage;
    }

    public boolean doesRegrow() {
        return crop.getRegrowStage() > 0;
    }

    public ItemStack harvest() {
        if (stage == crop.getStages()) {
            int cropMeta = crop.getCropMeta();
            int cropQuality = ((this.quality - 1) * 100);
            int cropSize = isGiantCrop ? 16000 : 0;
            if (crop.getRegrowStage() > 0) {
                stage = crop.getRegrowStage();
            }

            return new ItemStack(HMItems.crops, 1, cropSize + cropMeta + cropQuality);
        } else return null;
    }

    public void setHydrated() {
        daysWithoutWater = 0;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        crop = getCropFromOrdinal(nbt.getByte("CropMeta"));
        stage = nbt.getByte("CurrentStage");
        quality = nbt.getByte("CropQuality");
        isGiantCrop = nbt.getBoolean("IsGiant");
        isFertilized = nbt.getBoolean("IsFertilized");
        daysWithoutWater = nbt.getShort("DaysWithoutWater");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setByte("CropMeta", (byte) crop.getCropMeta());
        nbt.setByte("CurrentStage", (byte) stage);
        nbt.setByte("CropQuality", (byte) quality);
        nbt.setBoolean("IsGiant", isGiantCrop);
        nbt.setBoolean("IsFertilized", isFertilized);
        nbt.setShort("DaysWithoutWater", (short) daysWithoutWater);
    }

    /* Packet Based */
    public void toBytes(ByteBuf buf) {
        buf.writeByte(crop.getCropMeta());
        buf.writeByte(stage);
        buf.writeByte(quality);
        buf.writeBoolean(isGiantCrop);
        buf.writeBoolean(isFertilized);
    }

    public void fromBytes(ByteBuf buf) {
        crop = getCropFromOrdinal(buf.readByte());
        stage = buf.readByte();
        quality = buf.readByte();
        isGiantCrop = buf.readBoolean();
        isFertilized = buf.readBoolean();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        CropData other = (CropData) obj;
        if (crop == null) {
            if (other.crop != null) return false;
        } else if (!crop.equals(other.crop)) return false;
        if (isGiantCrop != other.isGiantCrop) return false;
        if (quality != other.quality) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((crop == null) ? 0 : crop.hashCode());
        result = prime * result + (isGiantCrop ? 1231 : 1237);
        result = prime * result + quality;
        return result;
    }
}