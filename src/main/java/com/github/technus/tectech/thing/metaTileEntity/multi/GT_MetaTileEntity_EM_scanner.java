package com.github.technus.tectech.thing.metaTileEntity.multi;

import com.github.technus.tectech.CommonValues;
import com.github.technus.tectech.TecTech;
import com.github.technus.tectech.mechanics.elementalMatter.core.cElementalInstanceStackMap;
import com.github.technus.tectech.mechanics.elementalMatter.core.stacks.cElementalDefinitionStack;
import com.github.technus.tectech.mechanics.elementalMatter.core.stacks.cElementalInstanceStack;
import com.github.technus.tectech.mechanics.elementalMatter.core.tElementalException;
import com.github.technus.tectech.recipe.TT_recipe;
import com.github.technus.tectech.thing.CustomItemList;
import com.github.technus.tectech.thing.block.QuantumGlassBlock;
import com.github.technus.tectech.thing.block.QuantumStuffBlock;
import com.github.technus.tectech.thing.item.ElementalDefinitionScanStorage_EM;
import com.github.technus.tectech.thing.metaTileEntity.IConstructable;
import com.github.technus.tectech.thing.metaTileEntity.hatch.GT_MetaTileEntity_Hatch_EnergyMulti;
import com.github.technus.tectech.thing.metaTileEntity.multi.base.*;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Energy;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Recipe;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.reflect.FieldUtils;

import static com.github.technus.tectech.CommonValues.V;
import static com.github.technus.tectech.CommonValues.VN;
import static com.github.technus.tectech.Util.StructureBuilderExtreme;
import static com.github.technus.tectech.Util.areBitsSet;
import static com.github.technus.tectech.loader.TecTechConfig.DEBUG_MODE;
import static com.github.technus.tectech.mechanics.elementalMatter.definitions.primitive.cPrimitiveDefinition.nbtE__;
import static com.github.technus.tectech.recipe.TT_recipe.E_RECIPE_ID;
import static com.github.technus.tectech.thing.casing.GT_Block_CasingsTT.textureOffset;
import static com.github.technus.tectech.thing.casing.TT_Container_Casings.sBlockCasingsTT;
import static com.github.technus.tectech.thing.metaTileEntity.multi.GT_MetaTileEntity_EM_crafting.crafter;
import static com.github.technus.tectech.thing.metaTileEntity.multi.em_machine.GT_MetaTileEntity_EM_machine.machine;
import static net.minecraft.util.StatCollector.translateToLocal;
import static net.minecraft.util.StatCollector.translateToLocalFormatted;

/**
 * Created by danie_000 on 17.12.2016.
 */
public class GT_MetaTileEntity_EM_scanner extends GT_MetaTileEntity_MultiblockBase_EM implements IConstructable {
    //region variables
    public static final int SCAN_DO_NOTHING = 0,
            SCAN_GET_NOMENCLATURE = 1, SCAN_GET_DEPTH_LEVEL = 2, SCAN_GET_AMOUNT = 4, SCAN_GET_CHARGE = 8,
            SCAN_GET_MASS = 16, SCAN_GET_ENERGY_LEVEL = 32, SCAN_GET_TIMESPAN_INFO = 64, SCAN_GET_ENERGY_STATES = 128,
            SCAN_GET_COLOR = 256, SCAN_GET_AGE = 512, SCAN_GET_TIMESPAN_MULT = 1024, SCAN_GET_CLASS_TYPE = 2048;

    private TT_recipe.TT_EMRecipe.TT_EMRecipe eRecipe;
    private cElementalDefinitionStack objectResearched;
    private cElementalInstanceStackMap objectsScanned;
    private String machineType;
    private long computationRemaining, computationRequired;
    private int[] scanComplexity;

    private String clientLocale = "en_US";
    //endregion

    //region structure
    private static final String[][] shape = new String[][]{
            {"     ", " 222 ", " 2.2 ", " 222 ", "     ",},
            {"00000", "00000", "00000", "00000", "00000",},
            {"00100", "01110", "11111", "01110", "00100",},
            {"01110", "1---1", "1---1", "1---1", "01110",},
            {"01110", "1---1", "1-A-1", "1---1", "01110",},
            {"01110", "1---1", "1---1", "1---1", "01110",},
            {"00100", "01110", "11\"11", "01110", "00100",},
            {"#####", "#000#", "#0!0#", "#000#", "#####",},
    };
    private static final Block[] blockType = new Block[]{sBlockCasingsTT, QuantumGlassBlock.INSTANCE, sBlockCasingsTT};
    private static final byte[] blockMeta = new byte[]{4, 0, 0};
    private final IHatchAdder[] addingMethods = new IHatchAdder[]{
            this::addClassicToMachineList,
            this::addElementalInputToMachineList,
            this::addElementalOutputToMachineList,
            this::addElementalMufflerToMachineList};
    private static final short[] casingTextures = new short[]{textureOffset, textureOffset + 4, textureOffset + 4, textureOffset + 4};
    private static final Block[] blockTypeFallback = new Block[]{sBlockCasingsTT, sBlockCasingsTT, sBlockCasingsTT, sBlockCasingsTT};
    private static final byte[] blockMetaFallback = new byte[]{0, 4, 4, 4};
    private static final String[] description = new String[]{
            EnumChatFormatting.AQUA + translateToLocal("tt.keyphrase.Hint_Details") + ":",
            translateToLocal("gt.blockmachines.multimachine.em.scanner.hint.0"),//1 - Classic Hatches or High Power Casing
            translateToLocal("gt.blockmachines.multimachine.em.scanner.hint.1"),//2 - Elemental Input Hatches or Molecular Casing
            translateToLocal("gt.blockmachines.multimachine.em.scanner.hint.2"),//3 - Elemental Output Hatches or Molecular Casing
            translateToLocal("gt.blockmachines.multimachine.em.scanner.hint.3"),//4 - Elemental Overflow Hatches or Molecular Casing
    };
    //endregion

    //region parameters
    private static final INameFunction<GT_MetaTileEntity_EM_scanner> CONFIG_NAME =
            (base, p) -> "Config at Depth: " + (p.hatchId() * 2 + p.parameterId());
    private static final IStatusFunction<GT_MetaTileEntity_EM_scanner> CONFIG_STATUS =
            (base, p) -> {
                double v = p.get();
                if (Double.isNaN(v)) {
                    return LedStatus.STATUS_WRONG;
                }
                v = (int) v;
                if (v == 0) return LedStatus.STATUS_NEUTRAL;
                if (v >= SCAN_GET_CLASS_TYPE) return LedStatus.STATUS_TOO_HIGH;
                if (v < 0) return LedStatus.STATUS_TOO_LOW;
                return LedStatus.STATUS_OK;
            };
    protected Parameters.Group.ParameterIn[] scanConfiguration;
    //endregion

    public GT_MetaTileEntity_EM_scanner(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
        eDismantleBoom = true;
    }

    public GT_MetaTileEntity_EM_scanner(String aName) {
        super(aName);
        eDismantleBoom = true;
    }

    private void quantumStuff(boolean shouldExist) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.getWorld() != null) {
            int xDir = ForgeDirection.getOrientation(base.getBackFacing()).offsetX * 4 + base.getXCoord();
            int yDir = ForgeDirection.getOrientation(base.getBackFacing()).offsetY * 4 + base.getYCoord();
            int zDir = ForgeDirection.getOrientation(base.getBackFacing()).offsetZ * 4 + base.getZCoord();
            Block block = base.getWorld().getBlock(xDir, yDir, zDir);
            if (shouldExist) {
                if (block != null && block.getMaterial() == Material.air) {
                    base.getWorld().setBlock(xDir, yDir, zDir, QuantumStuffBlock.INSTANCE, 0, 2);
                }
            } else {
                if (block instanceof QuantumStuffBlock) {
                    base.getWorld().setBlock(xDir, yDir, zDir, Blocks.air, 0, 2);
                }
            }
        }
    }

    private void addComputationRequirements(int depthPlus, int capabilities) {
        if (areBitsSet(SCAN_GET_NOMENCLATURE, capabilities)) {
            computationRequired += depthPlus * 5L;
            eRequiredData += depthPlus;
        }
        if (areBitsSet(SCAN_GET_DEPTH_LEVEL, capabilities)) {
            computationRequired += depthPlus * 10L;
            eRequiredData += depthPlus;

        }
        if (areBitsSet(SCAN_GET_AMOUNT, capabilities)) {
            computationRequired += depthPlus * 64L;
            eRequiredData += depthPlus * 8L;

        }
        if (areBitsSet(SCAN_GET_CHARGE, capabilities)) {
            computationRequired += depthPlus * 128L;
            eRequiredData += depthPlus * 4L;

        }
        if (areBitsSet(SCAN_GET_MASS, capabilities)) {
            computationRequired += depthPlus * 256L;
            eRequiredData += depthPlus * 4L;

        }
        if (areBitsSet(SCAN_GET_ENERGY_LEVEL, capabilities)) {
            computationRequired += depthPlus * 512L;
            eRequiredData += depthPlus * 16L;

        }
        if (areBitsSet(SCAN_GET_TIMESPAN_INFO, capabilities)) {
            computationRequired += depthPlus * 1024L;
            eRequiredData += depthPlus * 32L;

        }
        if (areBitsSet(SCAN_GET_ENERGY_STATES, capabilities)) {
            computationRequired += depthPlus * 2048L;
            eRequiredData += depthPlus * 32L;

        }
        if (areBitsSet(SCAN_GET_COLOR, capabilities)) {
            computationRequired += depthPlus * 1024L;
            eRequiredData += depthPlus * 48L;

        }
        if (areBitsSet(SCAN_GET_AGE, capabilities)) {
            computationRequired += depthPlus * 2048L;
            eRequiredData += depthPlus * 64L;

        }
        if (areBitsSet(SCAN_GET_TIMESPAN_MULT, capabilities)) {
            computationRequired += depthPlus * 2048L;
            eRequiredData += depthPlus * 64L;

        }
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_EM_scanner(mName);
    }

    @Override
    public boolean checkMachine_EM(IGregTechTileEntity iGregTechTileEntity, ItemStack itemStack) {
        if (!structureCheck_EM(shape, blockType, blockMeta, addingMethods, casingTextures, blockTypeFallback, blockMetaFallback, 2, 2, 0)) {
            return false;
        }
        return eInputHatches.size() == 1 && eOutputHatches.size() == 1 && eOutputHatches.get(0).getBaseMetaTileEntity().getFrontFacing() == iGregTechTileEntity.getFrontFacing();
    }

    @Override
    public boolean checkRecipe_EM(ItemStack itemStack) {
        eRecipe = null;
        if (!eInputHatches.isEmpty() && eInputHatches.get(0).getContainerHandler().hasStacks() && !eOutputHatches.isEmpty()) {
            cElementalInstanceStackMap researchEM = eInputHatches.get(0).getContainerHandler();
            if (ItemList.Tool_DataOrb.isStackEqual(itemStack, false, true)) {
                GT_Recipe scannerRecipe = null;
                for (cElementalInstanceStack stackEM : researchEM.values()) {
                    objectsScanned = null;
                    eRecipe = TT_recipe.TT_Recipe_Map_EM.sMachineRecipesEM.findRecipe(stackEM.definition);
                    if (eRecipe != null) {
                        scannerRecipe = eRecipe.scannerRecipe;
                        machineType = machine;
                        objectResearched = new cElementalDefinitionStack(stackEM.definition, 1);
                        //cleanMassEM_EM(objectResearched.getMass());
                        researchEM.remove(objectResearched.definition);
                        break;
                    }
                    eRecipe = TT_recipe.TT_Recipe_Map_EM.sCrafterRecipesEM.findRecipe(stackEM.definition);
                    if (eRecipe != null) {
                        scannerRecipe = eRecipe.scannerRecipe;
                        machineType = crafter;
                        objectResearched = new cElementalDefinitionStack(stackEM.definition, 1);
                        //cleanMassEM_EM(objectResearched.getMass());
                        researchEM.remove(objectResearched.definition);
                        break;
                    }
                    cleanStackEM_EM(stackEM);
                    researchEM.remove(stackEM.definition);
                }
                if (eRecipe != null && scannerRecipe != null) {//todo make sure it werks
                    computationRequired = computationRemaining = scannerRecipe.mDuration * 20L;
                    mMaxProgresstime = 20;//const
                    mEfficiencyIncrease = 10000;
                    eRequiredData = (short) (scannerRecipe.mSpecialValue >>> 16);
                    eAmpereFlow = (short) (scannerRecipe.mSpecialValue & 0xFFFF);
                    mEUt = scannerRecipe.mEUt;
                    quantumStuff(true);
                    return true;
                }
            } else if (CustomItemList.scanContainer.isStackEqual(itemStack, false, true)) {
                eRecipe = null;
                if (researchEM.hasStacks()) {
                    objectsScanned = researchEM.takeAllToNewMap();
                    cleanMassEM_EM(objectsScanned.getMass());

                    computationRequired = 0;
                    eRequiredData = 0;
                    eAmpereFlow = objectsScanned.size() + TecTech.RANDOM.next(objectsScanned.size());
                    mEUt = -(int) V[8];

                    //get depth scan complexity array
                    {
                        int[] scanComplexityTemp = new int[20];
                        for (int i = 0; i < 20; i++) {
                            scanComplexityTemp[i] = (int) scanConfiguration[i].get();
                        }
                        int maxDepth = 0;
                        for (int i = 0; i < 20; i++) {
                            if (scanComplexityTemp[i] != SCAN_DO_NOTHING) {
                                maxDepth = i;
                                if (!DEBUG_MODE) {
                                    scanComplexityTemp[i] &= ~SCAN_GET_CLASS_TYPE;
                                }
                                addComputationRequirements(i + 1, scanComplexityTemp[i]);
                            }
                        }
                        maxDepth += 1;//from index to len
                        scanComplexity = new int[maxDepth];
                        System.arraycopy(scanComplexityTemp, 0, scanComplexity, 0, maxDepth);
                    }

                    computationRemaining = computationRequired *= 20;
                    mMaxProgresstime = 20;//const
                    mEfficiencyIncrease = 10000;
                    quantumStuff(true);
                    return true;
                }
            }
        }
        quantumStuff(false);
        objectResearched = null;
        computationRemaining = 0;
        return false;
    }

    @Override
    public void outputAfterRecipe_EM() {
        if (eRecipe != null && ItemList.Tool_DataOrb.isStackEqual(mInventory[1], false, true)) {

            mInventory[1].setStackDisplayName(GT_LanguageManager.getTranslation(eRecipe.mOutputs[0].getDisplayName()) + ' ' + machineType + " Construction Data");
            NBTTagCompound tNBT = mInventory[1].getTagCompound();//code above makes it not null

            tNBT.setString("eMachineType", machineType);
            tNBT.setTag(E_RECIPE_ID, objectResearched.toNBT());
            tNBT.setString("author", EnumChatFormatting.BLUE + "Tec" + EnumChatFormatting.DARK_BLUE + "Tech" + EnumChatFormatting.WHITE + ' ' + machineType + " EM Recipe Generator");
        } else if (objectsScanned != null && CustomItemList.scanContainer.isStackEqual(mInventory[1], false, true)) {
            ElementalDefinitionScanStorage_EM.setContent(mInventory[1], objectsScanned, scanComplexity);
        }
        objectResearched = null;
        computationRemaining = 0;
        quantumStuff(false);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                CommonValues.TEC_MARK_EM,
                translateToLocal("gt.blockmachines.multimachine.em.scanner.desc.0"),//What is existing here?
                EnumChatFormatting.AQUA.toString() + EnumChatFormatting.BOLD + translateToLocal("gt.blockmachines.multimachine.em.scanner.desc.1")//I HAVE NO IDEA (yet)!
        };
    }

    @Override
    public String[] getInfoData() {
        long storedEnergy = 0;
        long maxEnergy = 0;
        for (GT_MetaTileEntity_Hatch_Energy tHatch : mEnergyHatches) {
            if (GT_MetaTileEntity_MultiBlockBase.isValidMetaTileEntity(tHatch)) {
                storedEnergy += tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy += tHatch.getBaseMetaTileEntity().getEUCapacity();
            }
        }
        for (GT_MetaTileEntity_Hatch_EnergyMulti tHatch : eEnergyMulti) {
            if (GT_MetaTileEntity_MultiBlockBase.isValidMetaTileEntity(tHatch)) {
                storedEnergy += tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy += tHatch.getBaseMetaTileEntity().getEUCapacity();
            }
        }

        return new String[]{
                translateToLocalFormatted("tt.keyphrase.Energy_Hatches", clientLocale) + ":",
                EnumChatFormatting.GREEN + Long.toString(storedEnergy) + EnumChatFormatting.RESET + " EU / " +
                        EnumChatFormatting.YELLOW + maxEnergy + EnumChatFormatting.RESET + " EU",
                (mEUt <= 0 ? translateToLocalFormatted("tt.keyphrase.Probably_uses", clientLocale) + ": " : translateToLocalFormatted("tt.keyphrase.Probably_makes", clientLocale) + ": ") +
                        EnumChatFormatting.RED + Math.abs(mEUt) + EnumChatFormatting.RESET + " EU/t " + translateToLocalFormatted("tt.keyword.at", clientLocale) + " " +
                        EnumChatFormatting.RED + eAmpereFlow + EnumChatFormatting.RESET + " A",
                translateToLocalFormatted("tt.keyphrase.Tier_Rating", clientLocale) + ": " + EnumChatFormatting.YELLOW + VN[getMaxEnergyInputTier_EM()] + EnumChatFormatting.RESET + " / " + EnumChatFormatting.GREEN + VN[getMinEnergyInputTier_EM()] + EnumChatFormatting.RESET +
                        " " + translateToLocalFormatted("tt.keyphrase.Amp_Rating", clientLocale) + ": " + EnumChatFormatting.GREEN + eMaxAmpereFlow + EnumChatFormatting.RESET + " A",
                translateToLocalFormatted("tt.keyword.Problems", clientLocale) + ": " + EnumChatFormatting.RED + (getIdealStatus() - getRepairStatus()) + EnumChatFormatting.RESET +
                        " " + translateToLocalFormatted("tt.keyword.Efficiency", clientLocale) + ": " + EnumChatFormatting.YELLOW + mEfficiency / 100.0F + EnumChatFormatting.RESET + " %",
                translateToLocalFormatted("tt.keyword.PowerPass", clientLocale) + ": " + EnumChatFormatting.BLUE + ePowerPass + EnumChatFormatting.RESET +
                        " " + translateToLocalFormatted("tt.keyword.SafeVoid", clientLocale) + ": " + EnumChatFormatting.BLUE + eSafeVoid,
                translateToLocalFormatted("tt.keyphrase.Computation_Available", clientLocale) + ": " + EnumChatFormatting.GREEN + eAvailableData + EnumChatFormatting.RESET + " / " + EnumChatFormatting.YELLOW + eRequiredData + EnumChatFormatting.RESET,
                translateToLocalFormatted("tt.keyphrase.Computation_Remaining", clientLocale) + ":",
                EnumChatFormatting.GREEN + Long.toString(computationRemaining / 20L) + EnumChatFormatting.RESET + " / " +
                        EnumChatFormatting.YELLOW + computationRequired / 20L
        };
    }

    @Override
    public void onRemoval() {
        quantumStuff(false);
        super.onRemoval();
    }

    @Override
    protected void parametersInstantiation_EM() {
        scanConfiguration = new Parameters.Group.ParameterIn[20];
        for (int i = 0; i < 10; i++) {
            Parameters.Group hatch = parametrization.getGroup(i);
            scanConfiguration[i * 2] = hatch.makeInParameter(0, 0, CONFIG_NAME, CONFIG_STATUS);
            scanConfiguration[i * 2 + 1] = hatch.makeInParameter(1, 0, CONFIG_NAME, CONFIG_STATUS);
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setLong("eComputationRemaining", computationRemaining);
        aNBT.setLong("eComputationRequired", computationRequired);
        if (objectResearched != null) {
            aNBT.setTag("eObject", objectResearched.toNBT());
        } else {
            aNBT.removeTag("eObject");
        }
        if (scanComplexity != null) {
            aNBT.setIntArray("eScanComplexity", scanComplexity);
        } else {
            aNBT.removeTag("eScanComplexity");
        }
        if (objectsScanned != null) {
            aNBT.setTag("eScanObjects", objectsScanned.toNBT());
        } else {
            aNBT.removeTag("eScanObjects");
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        computationRemaining = aNBT.getLong("eComputationRemaining");
        computationRequired = aNBT.getLong("eComputationRequired");
        if (aNBT.hasKey("eObject")) {
            objectResearched = cElementalDefinitionStack.fromNBT(aNBT.getCompoundTag("eObject"));
            if (objectResearched.definition == nbtE__) {
                objectResearched = null;
            }
        } else {
            objectResearched = null;
        }
        if (aNBT.hasKey("eScanComplexity")) {
            scanComplexity = aNBT.getIntArray("eScanComplexity");
        } else {
            scanComplexity = null;
        }
        try {
            if (aNBT.hasKey("eScanObjects")) {
                objectsScanned = cElementalInstanceStackMap.fromNBT(aNBT.getCompoundTag("eScanObjects"));
            }
        } catch (tElementalException e) {
            objectsScanned = new cElementalInstanceStackMap();
        }
    }

    @Override
    public void stopMachine() {
        quantumStuff(false);
        super.stopMachine();
        computationRequired = computationRemaining = 0;
        objectResearched = null;

    }

    @Override
    public void onFirstTick_EM(IGregTechTileEntity aBaseMetaTileEntity) {
        if (aBaseMetaTileEntity.isServerSide()) {
            if (computationRemaining > 0 && objectResearched != null) {
                eRecipe = null;
                if (ItemList.Tool_DataOrb.isStackEqual(mInventory[1], false, true)) {
                    eRecipe = TT_recipe.TT_Recipe_Map_EM.sMachineRecipesEM.findRecipe(objectResearched.definition);
                    if (eRecipe != null) {
                        machineType = machine;
                    } else {
                        eRecipe = TT_recipe.TT_Recipe_Map_EM.sCrafterRecipesEM.findRecipe(objectResearched.definition);
                        if (eRecipe != null) {
                            machineType = crafter;
                        }
                    }
                }
                if (eRecipe == null) {
                    quantumStuff(false);
                    objectResearched = null;
                    eRequiredData = 0;
                    computationRequired = computationRemaining = 0;
                    mMaxProgresstime = 0;
                    mEfficiencyIncrease = 0;
                } else {
                    quantumStuff(true);
                }
            }
        }
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (computationRemaining <= 0) {
            computationRemaining = 0;
            mProgresstime = mMaxProgresstime;
            return true;
        } else {
            computationRemaining -= eAvailableData;
            mProgresstime = 1;
            return super.onRunningTick(aStack);
        }
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        super.onRightclick(aBaseMetaTileEntity, aPlayer);

        if (!aBaseMetaTileEntity.isClientSide() && aPlayer instanceof EntityPlayerMP) {
            try {
                EntityPlayerMP player = (EntityPlayerMP) aPlayer;
                clientLocale = (String) FieldUtils.readField(player, "translator", true);
            } catch (Exception e) {
                clientLocale = "en_US";
            }
        } else {
            return true;
        }
        return true;
    }

    @Override
    public void onPreTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isActive() && (aTick & 0x2) == 0 && aBaseMetaTileEntity.isClientSide()) {
            int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX * 4 + aBaseMetaTileEntity.getXCoord();
            int yDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetY * 4 + aBaseMetaTileEntity.getYCoord();
            int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ * 4 + aBaseMetaTileEntity.getZCoord();
            aBaseMetaTileEntity.getWorld().markBlockRangeForRenderUpdate(xDir, yDir, zDir, xDir, yDir, zDir);
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void construct(int stackSize, boolean hintsOnly) {
        StructureBuilderExtreme(shape, blockType, blockMeta, 2, 2, 0, getBaseMetaTileEntity(), this, hintsOnly);
    }

    @Override
    public String[] getStructureDescription(int stackSize) {
        return description;
    }
}