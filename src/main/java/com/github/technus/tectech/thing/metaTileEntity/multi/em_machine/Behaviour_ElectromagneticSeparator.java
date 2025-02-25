package com.github.technus.tectech.thing.metaTileEntity.multi.em_machine;

import com.github.technus.tectech.TecTech;
import com.github.technus.tectech.mechanics.elementalMatter.core.cElementalInstanceStackMap;
import com.github.technus.tectech.mechanics.elementalMatter.core.stacks.cElementalInstanceStack;
import com.github.technus.tectech.mechanics.elementalMatter.definitions.complex.dAtomDefinition;
import com.github.technus.tectech.thing.metaTileEntity.multi.base.MultiblockControl;
import com.github.technus.tectech.thing.metaTileEntity.multi.base.INameFunction;
import com.github.technus.tectech.thing.metaTileEntity.multi.base.Parameters;
import com.github.technus.tectech.thing.metaTileEntity.multi.base.IStatusFunction;

import static com.github.technus.tectech.CommonValues.V;
import static com.github.technus.tectech.thing.metaTileEntity.multi.base.LedStatus.*;

/**
 * Created by danie_000 on 24.12.2017.
 */
public class Behaviour_ElectromagneticSeparator implements GT_MetaTileEntity_EM_machine.IBehaviour {
    private final byte tier;
    private int ticks;
    private byte precisionFull,precisionMinimal;
    private float maxCapacity;
    private long maxCharge;
    private int offsetMax;
    private Parameters.Group.ParameterIn fullSetting,minimalSetting,offsetSetting;
    private final static INameFunction<GT_MetaTileEntity_EM_machine> fullName= (gt_metaTileEntity_em_machine, iParameter) -> "Full Precision Input [e/3]";
    private final IStatusFunction<GT_MetaTileEntity_EM_machine> fullStatus= (gt_metaTileEntity_em_machine, iParameter) -> {
        double v=iParameter.get();
        if(Double.isNaN(v)){
            return STATUS_WRONG;
        }
        v=(int)v;
        if(Double.isInfinite(v) && v>0) {
            return STATUS_TOO_HIGH;
        }else if(v>precisionFull){
            return STATUS_HIGH;
        }else if(v<precisionFull){
            return STATUS_TOO_LOW;
        }
        return STATUS_OK;
    };
    private final static INameFunction<GT_MetaTileEntity_EM_machine> minimalName= (gt_metaTileEntity_em_machine, iParameter) -> "Minimal Precision Input [e/3]";
    private final IStatusFunction<GT_MetaTileEntity_EM_machine> minimalStatus= (gt_metaTileEntity_em_machine, iParameter) -> {
        double minimal=iParameter.get();
        double full=fullSetting.get();
        if(Double.isInfinite(minimal) && minimal>0) {
            return STATUS_TOO_HIGH;
        }else if(minimal>precisionMinimal){
            if(minimal>full){
                return STATUS_TOO_HIGH;
            }else {
                return STATUS_HIGH;
            }
        }else if(minimal==precisionMinimal){
            if(minimal>full){
                return STATUS_TOO_HIGH;
            }else {
                return STATUS_OK;
            }
        }else if(minimal<precisionMinimal){
            return STATUS_TOO_LOW;
        }else {
            return STATUS_WRONG;
        }
    };
    private final static INameFunction<GT_MetaTileEntity_EM_machine> offsetName= (gt_metaTileEntity_em_machine, iParameter) -> "Offset Input [e/3]";
    private final IStatusFunction<GT_MetaTileEntity_EM_machine> offsetStatus= (gt_metaTileEntity_em_machine, iParameter) -> {
        double offset=iParameter.get();
        if(offset>offsetMax){
            return STATUS_TOO_HIGH;
        }else if(offset>0){
            return STATUS_HIGH;
        }else if(offset==0){
            return STATUS_OK;
        }else if(offset>=-offsetMax){
            return STATUS_LOW;
        }else if(offset<-offsetMax){
            return STATUS_TOO_LOW;
        }else {
            return STATUS_WRONG;
        }
    };
    //private final static String[] DESCRIPTION_O =new String[]{"Full Precision Limit [e/3]","Minimal Precision Limit [e/3]","Offset Limit [e/3]",null,"Max Charge [e/3]","Max Capacity [eV/c^2]","Max Power Usage[EU/t]","Max Recipe Rime [tick]"};

    public Behaviour_ElectromagneticSeparator(int desiredTier){
        tier=(byte) desiredTier;
        ticks =Math.max(20,(1<<(12-desiredTier))*20);
        maxCapacity= dAtomDefinition.getSomethingHeavy().getMass()*(2<<tier);
        maxCharge=144*(1<<(tier-5));
        switch (tier){
            case 12:
                precisionFull=1;
                precisionMinimal =1;
                break;
            case 11:
                precisionFull=2;
                precisionMinimal =1;
                break;
            case 10:
                precisionFull=3;
                precisionMinimal =1;
                break;
            case 9:
                precisionFull=3;
                precisionMinimal =2;
                break;
            case 8:
                precisionFull=3;
                precisionMinimal =3;
                break;
            case 7:
                precisionFull=6;
                precisionMinimal =3;
                break;
            case 6:
                precisionFull=12;
                precisionMinimal =3;
                break;
            case 5:
                precisionFull=24;
                precisionMinimal =6;
                break;
            default: precisionFull= precisionMinimal =Byte.MAX_VALUE;
        }
        offsetMax=1<<((tier-8)<<1);
    }

    @Override
    public void parametersInstantiation(GT_MetaTileEntity_EM_machine te, Parameters parameters) {
        Parameters.Group hatch1=parameters.getGroup(7);
        fullSetting=hatch1.makeInParameter(0,0,fullName,fullStatus);
        minimalSetting=hatch1.makeInParameter(1,2,minimalName,minimalStatus);
        Parameters.Group hatch2=parameters.getGroup(8);
        offsetSetting=hatch2.makeInParameter(0,0,offsetName,offsetStatus);
    }

    @Override
    public boolean checkParametersInAndSetStatuses(GT_MetaTileEntity_EM_machine te, Parameters parameters) {
        return fullSetting.getStatus(true).isOk && minimalSetting.getStatus(true).isOk && offsetSetting.getStatus(true).isOk;
    }

    @Override
    public MultiblockControl<cElementalInstanceStackMap[]> process(cElementalInstanceStackMap[] inputs, GT_MetaTileEntity_EM_machine te, Parameters parameters) {
        cElementalInstanceStackMap input = inputs[0];
        if (input == null || input.isEmpty()) return null;//nothing in only valid input

        cElementalInstanceStack[] stacks = input.values();

        double inputMass = 0;
        for (cElementalInstanceStack stack : stacks) {
            inputMass += Math.abs(stack.getMass());
        }
        float excessMass = 0;
        while (inputMass > maxCapacity) {
            cElementalInstanceStack randomStack = stacks[TecTech.RANDOM.nextInt(stacks.length)];
            int amountToRemove = TecTech.RANDOM.nextInt((int) randomStack.getAmount()) + 1;
            randomStack.amount -= amountToRemove;//mutates the parent InstanceStackMap
            if (randomStack.amount <= 0) {
                input.remove(randomStack.definition);
            }
            float mass = Math.abs(randomStack.getDefinition().getMass()) * amountToRemove;
            excessMass += mass;
            inputMass -= mass;
        }

        long totalCharge=Math.abs(input.getCharge());
        if (totalCharge>maxCharge) return new MultiblockControl<>(excessMass);//AND THEN IT EXPLODES

        int mEut=(int)(((double)totalCharge/(double) maxCharge)*V[tier]);
        mEut = Math.max(mEut, 512);
        int mTicks=(int)(ticks*(inputMass/maxCapacity));
        mTicks=Math.max(mTicks,20);

        cElementalInstanceStackMap[] outputs = new cElementalInstanceStackMap[3];
        for (int i = 0; i < 3; i++) {
            outputs[i] = new cElementalInstanceStackMap();
        }

        double offsetIn=offsetSetting.get();
        double precisionFullIn=fullSetting.get();
        double precisionMinimalIn=minimalSetting.get();
        double levelsCountPlus1=precisionFullIn-precisionMinimalIn+1;

        //take all from hatch handler and put into new map - this takes from hatch to inner data storage
        stacks = input.takeAllToNewMap().values();//cleanup stacks
        for(cElementalInstanceStack stack:stacks){
            double charge=stack.definition.getCharge()-offsetIn;
            if(charge<precisionMinimalIn && charge>-precisionMinimalIn){
                outputs[1].putReplace(stack);
            }else if(charge>=precisionFullIn){
                outputs[2].putReplace(stack);
            }else if(charge<=-precisionFullIn){
                outputs[0].putReplace(stack);
            }else{
                long amount=(long)(stack.amount*((Math.abs(charge)-precisionMinimalIn+1)/levelsCountPlus1));//todo check
                if(amount>=stack.amount){
                    if(charge>0){
                        outputs[2].putReplace(stack);
                    }else {
                        outputs[0].putReplace(stack);
                    }
                }else {
                    cElementalInstanceStack clone=stack.clone();
                    clone.amount-=amount;
                    outputs[1].putReplace(clone);

                    stack.amount=amount;
                    if(charge>0){
                        outputs[2].putReplace(stack);
                    }else {
                        outputs[0].putReplace(stack);
                    }
                }
            }
        }

        return new MultiblockControl<>(outputs, mEut, 1+((int)Math.abs(offsetIn))/3, 0, 10000, mTicks, 0, excessMass);
    }
}
