package emu.grasscutter.game.ability.actions;

import com.google.protobuf.ByteString;
import emu.grasscutter.*;
import emu.grasscutter.data.binout.AbilityModifier.AbilityModifierAction;
import emu.grasscutter.game.ability.Ability;
import emu.grasscutter.game.entity.*;
import emu.grasscutter.game.props.FightProperty;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;

@AbilityAction(AbilityModifierAction.Type.HealHP)
public final class ActionHealHP extends AbilityActionHandler {
    @Override
    public boolean execute(
            Ability ability, AbilityModifierAction action, ByteString abilityData, GameEntity target) {
        var owner = ability.getOwner();
        // handle client gadgets, that the effective caster is the current local avatar
        if (owner instanceof EntityClientGadget ownerGadget) {
            owner =
                    ownerGadget
                            .getScene()
                            .getEntityById(ownerGadget.getOwnerEntityId()); // Caster for EntityClientGadget
            if (DebugConstants.LOG_ABILITIES) {
                Grasscutter.getLogger()
                        .debug(
                                "Owner {} has top owner {}: {}",
                                ability.getOwner(),
                                ownerGadget.getOwnerEntityId(),
                                owner);
            }
        }

        if (owner == null) return false;

        // Get all properties.
        var properties = new Object2FloatOpenHashMap<String>();
        // Add entity fight properties.
        for (var property : FightProperty.values()) {
            var name = property.name();
            var value = owner.getFightProperty(property);
            properties.put(name, value);
        }
        // Add ability properties.
        properties.putAll(ability.getAbilitySpecials());

        // Calculate ratios from properties.
        var amountByCasterMaxHPRatio = action.amountByCasterMaxHPRatio.get(properties, 0);
        var amountByCasterAttackRatio = action.amountByCasterAttackRatio.get(properties, 0);
        var amountByCasterCurrentHPRatio = action.amountByCasterCurrentHPRatio.get(properties, 0);
        var amountByTargetCurrentHPRatio = action.amountByTargetCurrentHPRatio.get(properties, 0);
        var amountByTargetMaxHPRatio = action.amountByTargetMaxHPRatio.get(properties, 0);

        Grasscutter.getLogger().trace("amountByCasterMaxHPRatio: " + amountByCasterMaxHPRatio);
        Grasscutter.getLogger().trace("amountByCasterAttackRatio: " + amountByCasterAttackRatio);
        Grasscutter.getLogger().trace("amountByCasterCurrentHPRatio: " + amountByCasterCurrentHPRatio);
        Grasscutter.getLogger().trace("amountByTargetCurrentHPRatio: " + amountByTargetCurrentHPRatio);
        Grasscutter.getLogger().trace("amountByTargetMaxHPRatio: " + amountByTargetMaxHPRatio);

        var amountToRegenerate = action.amount.get(ability);
        Grasscutter.getLogger().trace("Base amount: " + amountToRegenerate);

        amountToRegenerate +=
                amountByCasterMaxHPRatio * owner.getFightProperty(FightProperty.FIGHT_PROP_MAX_HP);
        amountToRegenerate +=
                amountByCasterAttackRatio * owner.getFightProperty(FightProperty.FIGHT_PROP_CUR_ATTACK);
        amountToRegenerate +=
                amountByCasterCurrentHPRatio * owner.getFightProperty(FightProperty.FIGHT_PROP_CUR_HP);

        Grasscutter.getLogger().trace("amountToRegenerate: " + amountToRegenerate);

        var abilityRatio = 1.0f;
        Grasscutter.getLogger().trace("Base abilityRatio: " + abilityRatio);

        if (!action.ignoreAbilityProperty)
            abilityRatio +=
                target.getFightProperty(FightProperty.FIGHT_PROP_HEAL_ADD)
                    + owner.getFightProperty(FightProperty.FIGHT_PROP_HEAL_ADD);

        Grasscutter.getLogger().trace("abilityRatio: " + abilityRatio);

        Grasscutter.getLogger().trace("Sub-regenerate amount: " + amountToRegenerate);
        amountToRegenerate +=
                amountByTargetCurrentHPRatio * target.getFightProperty(FightProperty.FIGHT_PROP_CUR_HP);
        amountToRegenerate +=
                amountByTargetMaxHPRatio * target.getFightProperty(FightProperty.FIGHT_PROP_MAX_HP);

        Grasscutter.getLogger().trace("Healing {} without ratios", amountToRegenerate);
        target.heal(
                amountToRegenerate * abilityRatio * action.healRatio.get(ability, 1f),
                action.muteHealEffect);

        return true;
    }
}
