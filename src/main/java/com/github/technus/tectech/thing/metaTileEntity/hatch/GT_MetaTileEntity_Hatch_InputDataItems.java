package com.github.technus.tectech.thing.metaTileEntity.hatch;

import com.github.technus.tectech.CommonValues;
import com.github.technus.tectech.Util;
import com.github.technus.tectech.mechanics.dataTransport.InventoryDataPacket;
import com.github.technus.tectech.thing.metaTileEntity.pipe.IConnectsToDataPipe;
import gregtech.api.enums.Dyes;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_DataAccess;
import gregtech.api.objects.GT_RenderedTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;

import static com.github.technus.tectech.CommonValues.MOVE_AT;
import static com.github.technus.tectech.recipe.TT_recipeAdder.nullItem;
import static com.github.technus.tectech.thing.metaTileEntity.hatch.GT_MetaTileEntity_Hatch_DataConnector.*;
import static gregtech.api.enums.Dyes.MACHINE_METAL;

public class GT_MetaTileEntity_Hatch_InputDataItems extends GT_MetaTileEntity_Hatch_DataAccess implements IConnectsToDataPipe {
    private final String mDescription;
    public boolean delDelay = true;
    private ItemStack[] stacks;

    public GT_MetaTileEntity_Hatch_InputDataItems(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier);
        Util.setTier(aTier,this);
        mDescription="ItemStack Data Input for Multiblocks";
    }

    public GT_MetaTileEntity_Hatch_InputDataItems(String aName, int aTier, String aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
        mDescription=aDescription;
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, new GT_RenderedTexture(EM_D_ACTIVE, Dyes.getModulation(getBaseMetaTileEntity().getColorization(), MACHINE_METAL.getRGBA())), new GT_RenderedTexture(EM_D_CONN)};
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, new GT_RenderedTexture(EM_D_SIDES, Dyes.getModulation(getBaseMetaTileEntity().getColorization(), MACHINE_METAL.getRGBA())), new GT_RenderedTexture(EM_D_CONN)};
    }

    @Override
    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return true;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Hatch_InputDataItems(this.mName, this.mTier, mDescription, this.mTextures);
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) {
            return true;
        } else {
            aBaseMetaTileEntity.openGUI(aPlayer);
            return true;
        }
    }

    @Override
    public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return null;
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return null;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isOutputFacing(byte aSide) {
        return false;
    }

    @Override
    public boolean isInputFacing(byte aSide) {
        return aSide == getBaseMetaTileEntity().getFrontFacing();
    }

    @Override
    public boolean isDataInputFacing(byte side) {
        return isInputFacing(side);
    }

    @Override
    public boolean canConnectData(byte side) {
        return isInputFacing(side);
    }

    @Override
    public IConnectsToDataPipe getNext(IConnectsToDataPipe source) {
        return null;
    }

    public void setContents(InventoryDataPacket iIn){
        if(iIn==null){
            stacks=null;
        }else{
            if(iIn.getContent().length>0) {
                stacks = iIn.getContent();
                delDelay=true;
            }else{
                stacks=null;
            }
        }
    }

    @Override
    public void onRemoval() {
        stacks=null;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        NBTTagCompound stacksTag=new NBTTagCompound();
        if(stacks!=null){
            stacksTag.setInteger("count",stacks.length);
            for(int i=0;i<stacks.length;i++){
                stacksTag.setTag(Integer.toString(i),stacks[i].writeToNBT(new NBTTagCompound()));
            }
        }
        aNBT.setTag("data_stacks",stacksTag);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        NBTTagCompound stacksTag=new NBTTagCompound();
        int count=stacksTag.getInteger("count");
        if(count>0){
            ArrayList<ItemStack> stacks=new ArrayList<>();
            for(int i=0;i<count;i++){
                ItemStack stack=ItemStack.loadItemStackFromNBT(stacksTag.getCompoundTag(Integer.toString(i)));
                if(stack!=null){
                    stacks.add(stack);
                }
            }
            if(stacks.size()>0) {
                this.stacks = stacks.toArray(nullItem);
            }
        }
    }

    @Override
    public int getSizeInventory() {
        return stacks!=null?stacks.length:0;
    }

    @Override
    public ItemStack getStackInSlot(int aIndex) {
        return stacks!=null && aIndex<stacks.length?stacks[aIndex]:null;
    }

    @Override
    public void onPreTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPreTick(aBaseMetaTileEntity,aTick);
        if(MOVE_AT == aTick % 20) {
            if(stacks==null){
                getBaseMetaTileEntity().setActive(false);
            } else {
                getBaseMetaTileEntity().setActive(true);
                if (delDelay) {
                    delDelay = false;
                } else {
                    setContents(null);
                }
            }
        }
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                CommonValues.TEC_MARK_EM,
                mDescription,
                "High speed fibre optics connector.",
                EnumChatFormatting.AQUA + "Must be painted to work"
        };
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    @Override
    public String[] getInfoData() {
        return new String[]{
                "Content: Stack Count: "+(stacks==null?0:stacks.length)
        };
    }

    @Override
    public byte getColorization() {
        return getBaseMetaTileEntity().getColorization();
    }
}
