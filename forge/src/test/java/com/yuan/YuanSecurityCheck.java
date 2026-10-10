package com.yuan;

import com.yuan.event.YuanSwordEvents;
import com.yuan.event.YuanDefenseState;
import com.yuan.event.YuanDroppedWeaponProtection;
import com.yuan.item.YuanAbsoluteAttack;
import com.yuan.item.YuanConfig;
import com.yuan.item.YuanWeaponBinding;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class YuanSecurityCheck {
    public static void main(String[] args) {
        CompoundTag input = new CompoundTag();
        input.putBoolean(YuanConfig.K_SNIPE, false);
        input.putFloat(YuanConfig.K_AOE_RANGE, Float.POSITIVE_INFINITY);
        input.putFloat(YuanConfig.K_REACH, -50.0F);
        input.putInt(YuanConfig.K_KILL_STRENGTH, 99);
        input.putString("unknown", "discard me");

        CompoundTag clean = YuanConfig.sanitize(input);
        assert !clean.getBoolean(YuanConfig.K_SNIPE);
        assert clean.getFloat(YuanConfig.K_AOE_RANGE) == 500.0F;
        assert clean.getFloat(YuanConfig.K_REACH) == 1.0F;
        assert clean.getInt(YuanConfig.K_KILL_STRENGTH) == 3;
        assert !clean.contains("unknown");
        assert YuanConfig.normalizeLegacyFloat(YuanConfig.K_TORMENT_PCT, 0.5F) == 50.0F;
        assert Math.abs(YuanConfig.normalizeLegacyFloat(YuanConfig.K_TIME_RANGE, 0.3F) - 30.0F) < 0.001F;
        assert YuanConfig.normalizeLegacyFloat(YuanConfig.K_TIME_RANGE, 1.0F) == 100.0F;
        assert YuanConfig.normalizeLegacyFloat(YuanConfig.K_TORMENT_PCT, 1.0F) == 1.0F;
        assert YuanConfig.normalizeLegacyFloat(YuanConfig.K_REACH, 0.5F) == 0.5F;

        CompoundTag combat = new CompoundTag();
        combat.putInt(YuanConfig.K_ABSOLUTE_REENTRY, 99);
        combat.putFloat(YuanConfig.K_CORRIDOR_DISTANCE, -1);
        combat.putFloat(YuanConfig.K_CORRIDOR_RADIUS, Float.NaN);
        combat.putInt(YuanConfig.K_RELEASE_MODE, -1);
        combat.putFloat(YuanConfig.K_PURGE_RANGE, Float.POSITIVE_INFINITY);
        combat.putInt(YuanConfig.K_MAX_ATTACK_TARGETS, 99999);
        combat.putInt(YuanConfig.K_ATTACK_ATTRIBUTE_MODE, 99);
        combat.putInt(YuanConfig.K_DEFENSE_SCOPE, 99);
        combat.putInt(YuanConfig.K_BINDING_MODE, -1);
        combat.putInt(YuanConfig.K_RECALL_GRACE_TICKS, 99999);
        combat.putInt(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS, -1);
        combat.putString(YuanConfig.K_ATTACK_PLAYERS, "wrong type");
        combat.putString(YuanConfig.K_DROP_DAMAGE_PROTECTION, "wrong type");
        combat.putBoolean("notApproved", true);
        CompoundTag sanitizedCombat = YuanConfig.sanitize(combat);
        assert sanitizedCombat.getInt(YuanConfig.K_ABSOLUTE_REENTRY) == 2;
        assert sanitizedCombat.getFloat(YuanConfig.K_CORRIDOR_DISTANCE) == 1;
        assert sanitizedCombat.getFloat(YuanConfig.K_CORRIDOR_RADIUS) == 3;
        assert sanitizedCombat.getInt(YuanConfig.K_RELEASE_MODE) == 0;
        assert sanitizedCombat.getFloat(YuanConfig.K_PURGE_RANGE) == 1024;
        assert sanitizedCombat.getInt(YuanConfig.K_MAX_ATTACK_TARGETS) == 4096;
        assert sanitizedCombat.getInt(YuanConfig.K_ATTACK_ATTRIBUTE_MODE) == 2;
        assert sanitizedCombat.getInt(YuanConfig.K_DEFENSE_SCOPE) == 2;
        assert sanitizedCombat.getInt(YuanConfig.K_BINDING_MODE) == 0;
        assert sanitizedCombat.getInt(YuanConfig.K_RECALL_GRACE_TICKS) == 1200;
        assert sanitizedCombat.getInt(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS) == 0;
        assert !sanitizedCombat.contains(YuanConfig.K_ATTACK_PLAYERS);
        assert !sanitizedCombat.contains(YuanConfig.K_DROP_DAMAGE_PROTECTION);
        assert !sanitizedCombat.contains("notApproved");

        CompoundTag forgedIdentity = new CompoundTag();
        forgedIdentity.putUUID(YuanWeaponBinding.WEAPON_UUID_TAG, UUID.randomUUID());
        forgedIdentity.putUUID(YuanWeaponBinding.OWNER_UUID_TAG, UUID.randomUUID());
        CompoundTag sanitizedIdentity = YuanConfig.sanitize(forgedIdentity);
        assert !sanitizedIdentity.contains(YuanWeaponBinding.WEAPON_UUID_TAG);
        assert !sanitizedIdentity.contains(YuanWeaponBinding.OWNER_UUID_TAG);

        assert !YuanWeaponBinding.configured(0);
        assert YuanWeaponBinding.configured(1);
        assert YuanWeaponBinding.blocksHostileDisarm(1);
        assert !YuanWeaponBinding.recalls(1);
        assert declaredMethod(YuanWeaponBinding.class, "recoversHostileDisarm");
        assert declaredMethod(YuanWeaponBinding.class, "canUseWeapon");
        assert declaredMethod(YuanWeaponBinding.class, "canUseWeaponPolicy");
        assert declaredMethod(YuanWeaponBinding.class, "repairOrUnbind");
        assert YuanWeaponBinding.canUseWeaponPolicy(0, false, false, false)
                : "mode 0 remains usable while unbound";
        assert !YuanWeaponBinding.canUseWeaponPolicy(0, true, false, false)
                : "disabled duplicates have no unbound capability";
        assert !YuanWeaponBinding.canUseWeaponPolicy(2, false, false, true)
                : "wrong-owner bound stacks have no capability";
        assert !YuanWeaponBinding.canUseWeaponPolicy(2, false, true, false)
                : "non-authoritative bound stacks have no capability";
        assert YuanWeaponBinding.canUseWeaponPolicy(2, false, true, true);
        assert YuanWeaponBinding.recalls(2);
        assert YuanWeaponBinding.blocksManualDrop(2, false);
        assert !YuanWeaponBinding.blocksManualDrop(2, true);
        assert YuanWeaponBinding.blocksContainerTransfer(3, false);
        assert YuanWeaponBinding.blocksPlayerTransfer(3, false);
        assert !YuanWeaponBinding.blocksContainerTransfer(2, true);
        assert YuanWeaponBinding.keepsOnDeath(2, true);
        assert !YuanWeaponBinding.keepsOnDeath(1, true);

        UUID bindingOwner = UUID.randomUUID();
        UUID transferOwner = UUID.randomUUID();
        UUID weaponId = UUID.randomUUID();
        CompoundTag identity = new CompoundTag();
        assert !YuanWeaponBinding.ensureIdentity(identity, bindingOwner, 0);
        assert !identity.contains(YuanWeaponBinding.WEAPON_UUID_TAG);
        assert YuanWeaponBinding.ensureIdentity(identity, bindingOwner, 2);
        assert weaponId(identity).equals(YuanWeaponBinding.weaponId(identity));
        assert bindingOwner.equals(YuanWeaponBinding.ownerId(identity));
        UUID assignedWeapon = YuanWeaponBinding.weaponId(identity);
        assert !YuanWeaponBinding.ensureIdentity(identity, UUID.randomUUID(), 2);
        assert assignedWeapon.equals(YuanWeaponBinding.weaponId(identity));
        assert bindingOwner.equals(YuanWeaponBinding.ownerId(identity));

        assert !YuanWeaponBinding.graceExpired(100, 139, 40);
        assert YuanWeaponBinding.graceExpired(100, 140, 40);
        assert !YuanWeaponBinding.restoreEligible(2, true, true, true, 100, 140, 40);
        assert !YuanWeaponBinding.restoreEligible(2, true, true, false, 100, 140, 40);
        assert !YuanWeaponBinding.restoreEligible(2, true, false, true, 100, 140, 40);
        assert YuanWeaponBinding.restoreEligible(2, true, false, false, 100, 140, 40);
        assert YuanWeaponBinding.restoreEligible(1, true, false, false, 100, 140, 40)
                : "mode 1 must recover hostile disarm from its trusted snapshot";
        assert !YuanWeaponBinding.restoreEligible(1, false, false, false, 100, 140, 40);
        assert !YuanWeaponBinding.restoreEligible(2, false, false, false, 100, 140, 40);
        assert !YuanWeaponBinding.restoreEligible(3, false, false, false, 100, 140, 40);
        assert YuanWeaponBinding.restoreEligible(3, true, false, false, 100, 140, 40);
        assert YuanWeaponBinding.authority(5, 2) == 2;
        assert YuanWeaponBinding.authority(2, 5) == 2;
        assert YuanWeaponBinding.authority(-1, 4) == 4;
        assert YuanWeaponBinding.authority(0, 100000) == 0;
        assert !YuanWeaponBinding.boundedCandidateScan(false, 32);
        assert !YuanWeaponBinding.boundedCandidateScan(true, 33);
        assert YuanWeaponBinding.boundedCandidateScan(true, 32);
        assert YuanWeaponBinding.canInitializeCandidate(true, null, bindingOwner);
        assert !YuanWeaponBinding.canInitializeCandidate(false, null, bindingOwner);
        assert !YuanWeaponBinding.canInitializeCandidate(false, UUID.randomUUID(), bindingOwner);
        assert YuanWeaponBinding.canInitializeCandidate("inventory", null, bindingOwner);
        assert !YuanWeaponBinding.canInitializeCandidate("remoteSlot", null, bindingOwner);
        assert !YuanWeaponBinding.canInitializeCandidate("itemEntity", null, bindingOwner);
        assert YuanWeaponBinding.legalPickup(true, null, transferOwner);
        assert !YuanWeaponBinding.legalPickup(false, bindingOwner, transferOwner);
        assert transferOwner.equals(YuanWeaponBinding.transferOwnerId(true, bindingOwner, transferOwner));
        assert bindingOwner.equals(YuanWeaponBinding.transferOwnerId(false, bindingOwner, transferOwner));
        assert transferOwner.equals(YuanWeaponBinding.transferOwnerId((UUID) null, transferOwner));
        assert YuanWeaponBinding.transferOwnerId((UUID) null, null) == null;
        CompoundTag pickupIdentity = new CompoundTag();
        assert YuanWeaponBinding.claimUnownedPickup(pickupIdentity, transferOwner, 2);
        assert YuanWeaponBinding.weaponId(pickupIdentity) != null;
        assert transferOwner.equals(YuanWeaponBinding.ownerId(pickupIdentity));
        assert !YuanWeaponBinding.claimUnownedPickup(new CompoundTag(), transferOwner, 0);
        assert YuanWeaponBinding.logoutRuntimeCleared(true);
        assert !YuanWeaponBinding.logoutRuntimeCleared(false);
        assert YuanWeaponBinding.loginReloadsPersisted(true);
        assert !YuanWeaponBinding.loginReloadsPersisted(false);
        assert YuanWeaponBinding.containerCustodyPolicy(true);
        assert !YuanWeaponBinding.containerCustodyPolicy(false);
        assert YuanWeaponBinding.markedCandidateIgnored(true, false);
        assert !YuanWeaponBinding.markedCandidateIgnored(true, true);
        assert YuanWeaponBinding.localReturnClearsMarker(true, true, bindingOwner, bindingOwner);
        assert !YuanWeaponBinding.localReturnClearsMarker(true, true, transferOwner, bindingOwner);
        assert !YuanWeaponBinding.localReturnClearsMarker(true, false, bindingOwner, bindingOwner);
        assert YuanWeaponBinding.noRestoreWhileLegalCustody(true);
        assert !YuanWeaponBinding.noRestoreWhileLegalCustody(false);
        assert !YuanWeaponBinding.localReturnEligible(false, true);
        assert YuanWeaponBinding.localReturnEligible(true, false);
        CompoundTag custodyStack = new CompoundTag();
        assert YuanWeaponBinding.applyContainerCustody(custodyStack, true);
        assert YuanWeaponBinding.isLegalCustody(custodyStack);
        assert YuanWeaponBinding.containerCustodyClearsAuthority(true, true);
        assert !YuanWeaponBinding.containerCustodyClearsAuthority(false, true);
        CompoundTag trustedCustody = YuanWeaponBinding.trustedSnapshot(custodyStack);
        assert YuanWeaponBinding.isLegalCustody(trustedCustody);
        assert !YuanWeaponBinding.bindingAuthorityAvailable(true, true);
        assert YuanWeaponBinding.bindingAuthorityAvailable(false, true);
        assert !YuanWeaponBinding.cloneRestoreAllowed(true, true);
        assert YuanWeaponBinding.cloneRestoreAllowed(false, true);
        assert !YuanWeaponBinding.cloneRestoreAllowed(false, false);
        assert !YuanWeaponBinding.restoreEligible(2, true, false, false, 100, 140, 40, true);

        CompoundTag serializedStack = new CompoundTag();
        serializedStack.putUUID(YuanWeaponBinding.WEAPON_UUID_TAG, assignedWeapon);
        serializedStack.putUUID(YuanWeaponBinding.OWNER_UUID_TAG, bindingOwner);
        YuanWeaponBinding.applyContainerCustody(serializedStack, true);
        CompoundTag serializedRecord = YuanWeaponBinding.serializeBindingRecord(serializedStack, 100, 120, 160);
        CompoundTag loadedRecord = YuanWeaponBinding.loadBindingRecord(serializedRecord, bindingOwner);
        assert !loadedRecord.isEmpty();
        assert loadedRecord.getLong("MissingSince") == 120;
        assert loadedRecord.getLong("LifecycleUntil") == 160;
        assert YuanWeaponBinding.isLegalCustody(loadedRecord.getCompound("Stack"));
        CompoundTag malformedRecord = YuanWeaponBinding.serializeBindingRecord(new CompoundTag(), 100, 120, 160);
        assert YuanWeaponBinding.loadBindingRecord(malformedRecord, bindingOwner).isEmpty();
        assert YuanWeaponBinding.loadBindingRecord(serializedRecord, transferOwner).isEmpty();

        YuanWeaponBinding.CustodyState custodyState = YuanWeaponBinding.applyContainerCustodyState(new CompoundTag(), true);
        assert custodyState.marked() && custodyState.clearAuthority();
        assert YuanWeaponBinding.isLegalCustody(custodyState.stack());
        assert YuanWeaponBinding.shouldNotifyContainer(custodyState, true);
        assert !YuanWeaponBinding.shouldNotifyContainer(custodyState, false);
        assert !YuanWeaponBinding.shouldNotifyContainer(
                new YuanWeaponBinding.CustodyState(new CompoundTag(), false, false), true);
        assert !YuanWeaponBinding.shouldRestoreCloneSnapshot(custodyState.stack(), 2, true, Long.MIN_VALUE, 100);
        CompoundTag unmarkedSnapshot = new CompoundTag();
        assert YuanWeaponBinding.shouldRestoreCloneSnapshot(unmarkedSnapshot, 2, true, 99, 100);
        assert !YuanWeaponBinding.shouldRestoreCloneSnapshot(unmarkedSnapshot, 2, true, 100, 100);
        assert !YuanWeaponBinding.shouldRestoreCloneSnapshot(unmarkedSnapshot, 1, true, Long.MIN_VALUE, 100);
        assert !YuanWeaponBinding.shouldRestoreCloneSnapshot(unmarkedSnapshot, 2, false, Long.MIN_VALUE, 100);
        assert YuanWeaponBinding.tombstoneAllowsSnapshot(Long.MIN_VALUE, 100);
        assert !YuanWeaponBinding.tombstoneAllowsSnapshot(100, 100);
        assert !YuanWeaponBinding.tombstoneAllowsSnapshot(101, 100);
        assert YuanWeaponBinding.tombstoneAllowsSnapshot(99, 100);

        YuanWeaponBinding.LogoutState logoutState = YuanWeaponBinding.logoutRecordState(serializedRecord);
        assert logoutState.clearRuntime();
        assert logoutState.persisted().getLong("LifecycleUntil") == Long.MAX_VALUE;
        assert logoutState.persisted().getCompound("Stack").equals(serializedStack);

        CompoundTag oldPlayerData = savedBinding(bindingOwner, assignedWeapon, 100, 120, 160);
        assert YuanWeaponBinding.hasPersistedRecord(oldPlayerData);
        YuanWeaponBinding.clearPersistedRecord(oldPlayerData);
        assert !YuanWeaponBinding.hasPersistedRecord(oldPlayerData);

        CompoundTag transferSource = savedBinding(bindingOwner, assignedWeapon, 100, 120, 160);
        CompoundTag transferTarget = new CompoundTag();
        YuanWeaponBinding.transferPersistedRecord(transferSource, transferTarget, transferOwner);
        assert !YuanWeaponBinding.hasPersistedRecord(transferSource);
        assert YuanWeaponBinding.hasPersistedRecord(transferTarget);
        assert transferOwner.equals(YuanWeaponBinding.persistedOwner(transferTarget));

        assert YuanWeaponBinding.authorityOwnerMatches(bindingOwner, bindingOwner);
        assert !YuanWeaponBinding.authorityOwnerMatches(bindingOwner, transferOwner);
        assert YuanWeaponBinding.persistedMissingSince(savedBinding(bindingOwner, assignedWeapon, 100, 120, 160)) == 120;
        assert YuanWeaponBinding.persistedLifecycleUntil(savedBinding(bindingOwner, assignedWeapon, 100, 120, 160)) == 160;
        assert YuanWeaponBinding.loginMissingSince(120, 500) == 120;
        assert YuanWeaponBinding.loginMissingSince(-1, 500) == -1;

        CompoundTag reloadableContainerRecord = savedBinding(bindingOwner, assignedWeapon, 100, 120, 160);
        assert YuanWeaponBinding.shouldEnforceContainer(
                YuanWeaponBinding.hasPersistedRecord(reloadableContainerRecord), false);
        assert !YuanWeaponBinding.shouldEnforceContainer(
                YuanWeaponBinding.hasPersistedRecord(reloadableContainerRecord), true);
        assert YuanWeaponBinding.legalTransferAllowed(2, true);
        assert !YuanWeaponBinding.legalTransferAllowed(2, false);
        assert YuanWeaponBinding.ignoreLegalTransfer(true, false);
        assert !YuanWeaponBinding.ignoreLegalTransfer(false, false);
        assert !YuanWeaponBinding.ignoreLegalTransfer(true, true);
        CompoundTag tombstones = new CompoundTag();
        YuanWeaponBinding.markOfflineTransfer(tombstones, bindingOwner, assignedWeapon);
        assert YuanWeaponBinding.hasOfflineTransfer(tombstones, bindingOwner, assignedWeapon);
        assert !YuanWeaponBinding.hasOfflineTransfer(tombstones, transferOwner, assignedWeapon);
        assert YuanWeaponBinding.shouldWriteOfflineTombstone(true);
        assert !YuanWeaponBinding.shouldWriteOfflineTombstone(false);
        assert YuanWeaponBinding.tombstoneRejectsSnapshot(200, 100);
        assert YuanWeaponBinding.tombstoneRejectsSnapshot(100, 100);
        assert !YuanWeaponBinding.tombstoneRejectsSnapshot(100, 101);
        YuanWeaponBinding.markOfflineTransfer(tombstones, transferOwner, assignedWeapon, 200);
        YuanWeaponBinding.clearOfflineTransfer(tombstones, bindingOwner, assignedWeapon);
        assert !YuanWeaponBinding.hasOfflineTransfer(tombstones, bindingOwner, assignedWeapon);
        assert YuanWeaponBinding.shouldClearOfflineRecord(true);
        assert !YuanWeaponBinding.shouldClearOfflineRecord(false);

        CompoundTag strictIntegers = new CompoundTag();
        strictIntegers.putFloat(YuanConfig.K_ABSOLUTE_REENTRY, 1);
        strictIntegers.putDouble(YuanConfig.K_RELEASE_MODE, 2);
        strictIntegers.putLong(YuanConfig.K_MAX_ATTACK_TARGETS, Long.MAX_VALUE);
        strictIntegers.putLong(YuanConfig.K_RECALL_GRACE_TICKS, Long.MIN_VALUE);
        strictIntegers.putByte(YuanConfig.K_DEFENSE_SCOPE, (byte)1);
        strictIntegers.putShort(YuanConfig.K_BINDING_MODE, (short)2);
        CompoundTag sanitizedIntegers = YuanConfig.sanitize(strictIntegers);
        assert !sanitizedIntegers.contains(YuanConfig.K_ABSOLUTE_REENTRY);
        assert !sanitizedIntegers.contains(YuanConfig.K_RELEASE_MODE);
        assert sanitizedIntegers.getInt(YuanConfig.K_MAX_ATTACK_TARGETS) == 4096;
        assert sanitizedIntegers.getInt(YuanConfig.K_RECALL_GRACE_TICKS) == 0;
        assert sanitizedIntegers.getInt(YuanConfig.K_DEFENSE_SCOPE) == 1;
        assert sanitizedIntegers.getInt(YuanConfig.K_BINDING_MODE) == 2;

        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        assert YuanSwordEvents.shouldCounter(true, false, false, true);
        assert !YuanSwordEvents.shouldCounter(true, true, false, true);
        assert !YuanSwordEvents.shouldCounter(true, false, true, true);
        assert !YuanSwordEvents.shouldCounter(true, false, false, false)
                : "counterattack must honor shared target exclusion";
        assert YuanSwordEvents.shouldGrantFlight(true, false);
        assert !YuanSwordEvents.shouldGrantFlight(true, true);
        assert YuanSwordEvents.shouldRevokeFlight(false, true, false, false);
        assert !YuanSwordEvents.shouldRevokeFlight(false, true, true, false);
        assert YuanSwordEvents.capabilityEnabled(true, true);
        assert !YuanSwordEvents.capabilityEnabled(true, false)
                : "configStack consumers require weapon authority";

        assert YuanDefenseState.scopeSlot(0, true, true, true) == 0;
        assert YuanDefenseState.scopeSlot(0, false, true, true) == -1;
        assert YuanDefenseState.scopeSlot(1, false, true, true) == 1;
        assert YuanDefenseState.scopeSlot(2, false, false, true) == 2;
        assert YuanDefenseState.scopeSlot(2, false, false, false) == -1;
        assert YuanDefenseState.scopeAllows(0, 0);
        assert !YuanDefenseState.scopeAllows(0, 1);
        assert YuanDefenseState.scopeAllows(1, 1);
        assert YuanDefenseState.scopeAllows(2, 2);
        assert YuanDefenseState.activeDefense(false, true, true, true, false);
        assert !YuanDefenseState.activeDefense(false, true, false, true, false);
        assert !YuanDefenseState.activeDefense(false, false, true, true, false);
        assert !YuanDefenseState.activeDefense(true, true, true, true, true);

        assert YuanDefenseState.recoveryHealth(12, 8, 20) == 12;
        assert YuanDefenseState.recoveryHealth(8, 12, 20) == 12;
        assert YuanDefenseState.recoveryHealth(16, 12, 20) == 16;
        assert YuanDefenseState.recoveryHealth(0, 8, 20) == 8;
        assert YuanDefenseState.recoveryHealth(Float.NaN, -1, 20) == 20;
        assert YuanDefenseState.recoveryHealth(Float.POSITIVE_INFINITY, Float.NaN, 20) == 20;
        assert YuanDefenseState.shouldBlockHealthSet(true, true, false, false, 20, 10);
        assert YuanDefenseState.shouldBlockHealthSet(true, true, false, 20, Float.NaN);
        assert !YuanDefenseState.shouldBlockHealthSet(true, true, false, 10, 20);
        assert !YuanDefenseState.shouldBlockHealthSet(false, true, false, 20, 0);
        assert !YuanDefenseState.shouldBlockHealthSet(true, true, true, 20, 0);

        Object healthTarget = new Object();
        Object otherHealthTarget = new Object();
        assert !YuanDefenseState.healthDecreaseAllowed(healthTarget);
        YuanDefenseState.authorizeHealthDecrease(healthTarget);
        assert YuanDefenseState.consumeHealthDecrease(healthTarget);
        assert !YuanDefenseState.consumeHealthDecrease(healthTarget)
                : "hurt authorization is consumed exactly once";
        YuanDefenseState.authorizeHealthDecrease(healthTarget);
        YuanDefenseState.authorizeHealthDecrease(healthTarget);
        assert YuanDefenseState.consumeHealthDecrease(healthTarget);
        assert YuanDefenseState.consumeHealthDecrease(healthTarget)
                : "nested authorization is depth-safe";
        assert !YuanDefenseState.consumeHealthDecrease(healthTarget);
        YuanDefenseState.authorizeHealthDecrease(healthTarget);
        assert !YuanDefenseState.consumeHealthDecrease(otherHealthTarget)
                : "authorization is identity-scoped";
        assert YuanDefenseState.consumeHealthDecrease(healthTarget);
        try (YuanDefenseState.Scope outer = YuanDefenseState.allowHealthDecrease(healthTarget)) {
            assert YuanDefenseState.healthDecreaseAllowed(healthTarget);
            assert !YuanDefenseState.healthDecreaseAllowed(otherHealthTarget);
            assert !YuanDefenseState.shouldBlockHealthSet(true, true, false,
                    YuanDefenseState.healthDecreaseAllowed(healthTarget), 20, 10);
            assert YuanDefenseState.shouldBlockHealthSet(true, true, false,
                    YuanDefenseState.healthDecreaseAllowed(otherHealthTarget), 20, 10);
            try (YuanDefenseState.Scope inner = YuanDefenseState.allowHealthDecrease(healthTarget)) {
                assert YuanDefenseState.healthDecreaseAllowed(healthTarget);
            }
            assert YuanDefenseState.healthDecreaseAllowed(healthTarget);
        }
        assert !YuanDefenseState.healthDecreaseAllowed(healthTarget);
        try {
            try (YuanDefenseState.Scope ignored = YuanDefenseState.allowHealthDecrease(healthTarget)) {
                throw new IllegalStateException("scope cleanup");
            }
        } catch (IllegalStateException expected) {
            assert expected.getMessage().equals("scope cleanup");
        }
        assert !YuanDefenseState.healthDecreaseAllowed(healthTarget);

        UUID session = UUID.randomUUID();
        YuanDefenseState.updateSession(session, true, 12, 20, 2);
        assert YuanDefenseState.healthBaseline(session) == 12;
        assert YuanDefenseState.absorptionBaseline(session) == 2;
        YuanDefenseState.updateSession(session, true, 5, 20, 1);
        assert YuanDefenseState.healthBaseline(session) == 12;
        assert YuanDefenseState.absorptionBaseline(session) == 2;
        YuanDefenseState.updateSession(session, true, 18, 20, 4);
        assert YuanDefenseState.healthBaseline(session) == 18;
        assert YuanDefenseState.absorptionBaseline(session) == 4;
        YuanDefenseState.updateSession(session, false, 18, 20, 4);
        assert Float.isNaN(YuanDefenseState.healthBaseline(session));
        assert YuanDefenseState.absorptionBaseline(session) == 0;

        assert YuanDefenseState.allowsDefense(true, false);
        try (YuanAbsoluteAttack.Scope ignored = YuanAbsoluteAttack.enter(null)) {
            assert !YuanDefenseState.allowsDefense(true, YuanAbsoluteAttack.isActive(null));
        }
        assert !YuanDefenseState.allowsDefense(false, false);

        assert YuanDefenseState.protectRemoval(true, true, false, false, true, false,
                Entity.RemovalReason.DISCARDED) == false
                : "privileged command removal bypass is independent of binding admin config";
        assert YuanDefenseState.protectRemoval(true, true, true, false, false, false,
                Entity.RemovalReason.KILLED);
        assert YuanDefenseState.protectRemoval(true, true, true, false, false, false,
                Entity.RemovalReason.DISCARDED);
        assert !YuanDefenseState.protectRemoval(false, true, true, false, false, false,
                Entity.RemovalReason.KILLED);
        assert !YuanDefenseState.protectRemoval(true, true, true, true, false, false,
                Entity.RemovalReason.KILLED);
        assert !YuanDefenseState.protectRemoval(true, true, true, false, true, false,
                Entity.RemovalReason.DISCARDED);
        assert !YuanDefenseState.protectRemoval(true, true, false, false, true, false,
                Entity.RemovalReason.DISCARDED);
        assert !YuanDefenseState.protectRemoval(true, true, true, false, false, true,
                Entity.RemovalReason.DISCARDED);
        assert !YuanDefenseState.protectRemoval(true, true, true, false, false, false,
                Entity.RemovalReason.CHANGED_DIMENSION);
        assert !YuanDefenseState.protectRemoval(true, true, true, false, false, false,
                Entity.RemovalReason.UNLOADED_TO_CHUNK);

        assert !YuanDefenseState.administrativeCommand();
        try (YuanDefenseState.Scope ignored = YuanDefenseState.enterAdministrativeCommand()) {
            assert YuanDefenseState.administrativeCommand();
        }
        assert !YuanDefenseState.administrativeCommand();
        assert !YuanDefenseState.lifecycleRemoval();
        try (YuanDefenseState.Scope ignored = YuanDefenseState.enterLifecycleRemoval()) {
            assert YuanDefenseState.lifecycleRemoval();
        }
        assert !YuanDefenseState.lifecycleRemoval();

        assert YuanDefenseState.shouldBlockDamage(true, true, false, true, false, false, 0);
        assert !YuanDefenseState.shouldBlockDamage(true, true, false, true, false, false, 1);
        try (YuanDefenseState.Scope ignored = YuanDefenseState.allowHealthDecrease(healthTarget)) {
            assert !YuanDefenseState.shouldBlockHealthSet(true, true, false,
                    YuanDefenseState.healthDecreaseAllowed(healthTarget), 20, 10);
        }
        assert YuanDefenseState.shouldBlockDamage(true, true, false, false, true, false, 1);
        assert !YuanDefenseState.shouldBlockDamage(true, true, false, true, false, false, 2);
        assert YuanDefenseState.shouldBlockDamage(true, true, false, false, false, true, 2);
        assert !YuanDefenseState.shouldBlockDamage(false, true, false, true, true, true, 0);
        assert !YuanDefenseState.shouldBlockDamage(true, true, true, true, true, true, 0);

        assert YuanDefenseState.safePositionDecision(true, true, true, true, true, true);
        assert !YuanDefenseState.safePositionDecision(false, true, true, true, true, true);
        assert !YuanDefenseState.safePositionDecision(true, false, true, true, true, true);
        assert !YuanDefenseState.safePositionDecision(true, true, false, true, true, true);
        assert !YuanDefenseState.safePositionDecision(true, true, true, false, true, true);
        assert !YuanDefenseState.safePositionDecision(true, true, true, true, false, true);
        assert !YuanDefenseState.safePositionDecision(true, true, true, true, true, false);
        assert YuanDefenseState.supportedRescuePosition(true, true, true, true, true, true, true);
        assert !YuanDefenseState.supportedRescuePosition(false, true, true, true, true, true, true)
                : "a final holding destination requires solid support";
        assert !YuanDefenseState.supportedRescuePosition(true, false, true, true, true, true, true);
        assert !YuanDefenseState.supportedRescuePosition(true, true, false, true, true, true, true);
        assert !YuanDefenseState.supportedRescuePosition(true, true, true, false, true, true, true);
        assert !YuanDefenseState.supportedRescuePosition(true, true, true, true, false, true, true);
        assert !YuanDefenseState.supportedRescuePosition(true, true, true, true, true, false, true);
        assert !YuanDefenseState.supportedRescuePosition(true, true, true, true, true, true, false)
                : "rescue must never load or accept an unloaded coordinate";
        assert YuanDefenseState.suspensionPosition(true, true, true, true, true, true)
                : "temporary no-gravity hold may be unsupported but must be otherwise verified";
        assert !YuanDefenseState.suspensionPosition(true, true, true, true, true, false);

        assert YuanDefenseState.voidFallback(true, true) == 0;
        assert YuanDefenseState.voidFallback(false, true) == 1;
        assert YuanDefenseState.voidFallback(false, false) == 2;
        assert YuanDefenseState.rescueY(0, -64) >= -63
                : "rescue handling must never leave a player below minimum height";
        YuanDefenseState.RescueDecision hold = YuanDefenseState.terminalRescueDecision(false, true,
                false, false, -100, -64);
        assert hold.state() == YuanDefenseState.RescueState.HOLD_RETRY;
        assert hold.y() >= -63;
        assert hold.targetAvailable() && hold.retry() && hold.enableNoGravity()
                && hold.ownsNoGravity() && hold.zeroVelocity();
        assert hold.equals(YuanDefenseState.terminalRescueDecision(false, true,
                false, false, -100, -64))
                : "terminal hold must remain stable while no supported destination exists";
        YuanDefenseState.RescueDecision externalGravity = YuanDefenseState.terminalRescueDecision(false, true,
                true, false, -100, -64);
        assert externalGravity.retry() && externalGravity.targetAvailable();
        assert !externalGravity.enableNoGravity() && !externalGravity.ownsNoGravity()
                : "pre-existing noGravity still retries without transferring ownership to Yuan";
        YuanDefenseState.RescueDecision rescued = YuanDefenseState.terminalRescueDecision(true, true,
                false, false, 80, -64);
        assert rescued.state() == YuanDefenseState.RescueState.RESCUED;
        assert !rescued.retry() && !rescued.enableNoGravity() && rescued.zeroVelocity()
                : "supported rescue restores gravity and stops residual motion";
        assert YuanDefenseState.cleanupVoidState(true, true, true).clearRetry();
        assert YuanDefenseState.cleanupVoidState(true, true, true).clearNoGravity();
        assert YuanDefenseState.cleanupVoidState(true, true, true).clearNoPhysics();
        assert YuanDefenseState.cleanupVoidState(true, false, false).clearRetry();
        assert !YuanDefenseState.cleanupVoidState(true, false, false).clearNoGravity()
                : "cleanup preserves externally-owned noGravity";
        assert !YuanDefenseState.cleanupVoidState(true, false, false).clearNoPhysics()
                : "cleanup preserves externally-owned noPhysics";
        YuanDefenseState.RescueDecision phase = YuanDefenseState.terminalRescueDecision(false, false,
                false, false, -100, -64);
        assert phase.state() == YuanDefenseState.RescueState.PHASE_HOLD;
        assert phase.targetAvailable() && phase.retry() && phase.enableNoGravity()
                && phase.enableNoPhysics() && phase.ownsNoGravity() && phase.ownsNoPhysics();
        YuanDefenseState.RescueDecision externalPhase = YuanDefenseState.terminalRescueDecision(false, false,
                true, true, -100, -64);
        assert !externalPhase.enableNoGravity() && !externalPhase.enableNoPhysics();
        assert !externalPhase.ownsNoGravity() && !externalPhase.ownsNoPhysics();
        YuanDefenseState.EmergencyTarget emergency = YuanDefenseState.emergencyTarget(
                Double.NaN, 1000, 4, 6, -10, 10, -64, 320);
        assert emergency != null && Double.isFinite(emergency.x()) && Double.isFinite(emergency.z());
        assert emergency.x() >= -10 && emergency.x() <= 10;
        assert emergency.z() >= -10 && emergency.z() <= 10;
        assert emergency.y() >= -63 && emergency.y() < 320;
        assert YuanSwordEvents.rescueResult(true) == YuanSwordEvents.RescueResult.RESCUED;
        assert YuanSwordEvents.rescueResult(false) == YuanSwordEvents.RescueResult.HOLD_RETRY;

        assert YuanDroppedWeaponProtection.protectsDamage(true, true, true);
        assert !YuanDroppedWeaponProtection.protectsDamage(false, true, true);
        assert !YuanDroppedWeaponProtection.protectsDamage(true, false, true);
        assert !YuanDroppedWeaponProtection.protectsDamage(true, true, false);
        assert YuanDroppedWeaponProtection.preventNaturalExpiry(true, true, false);
        assert !YuanDroppedWeaponProtection.preventNaturalExpiry(true, true, true);
        assert !YuanDroppedWeaponProtection.preventNaturalExpiry(false, true, false);
        assert !YuanDroppedWeaponProtection.preventNaturalExpiry(true, false, false);
        assert YuanDroppedWeaponProtection.shouldRescueBeforeVanilla(true, false, true, true);
        assert !YuanDroppedWeaponProtection.shouldRescueBeforeVanilla(true, true, true, true);
        assert !YuanDroppedWeaponProtection.shouldRescueBeforeVanilla(false, false, true, true);
        assert !YuanDroppedWeaponProtection.shouldRescueBeforeVanilla(true, false, false, true);
        assert !YuanDroppedWeaponProtection.shouldRescueBeforeVanilla(true, false, true, false);
        assert YuanDroppedWeaponProtection.rescueDestination(true, true, true, true) == 1;
        assert YuanDroppedWeaponProtection.rescueDestination(true, true, true, false) == 2;
        assert YuanDroppedWeaponProtection.rescueDestination(true, true, false, true) == 2;
        assert YuanDroppedWeaponProtection.rescueDestination(true, false, true, true) == 2;
        assert YuanDroppedWeaponProtection.rescueDestination(false, false, false, false) == 2;
        assert YuanDroppedWeaponProtection.usesProtectedEntity(true);
        assert !YuanDroppedWeaponProtection.usesProtectedEntity(false);
        assert declaredMethod(YuanDroppedWeaponProtection.class, "shouldReplaceJoinedItem");
        assert declaredMethod(com.yuan.item.YuanSwordItem.class, "hasCustomEntity");
        assert declaredMethod(com.yuan.item.YuanSwordItem.class, "createEntity");
        Class<?> protectedItem = java.util.Arrays.stream(YuanDroppedWeaponProtection.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("ProtectedItemEntity")).findFirst().orElseThrow();
        assert !declaredMethod(protectedItem, "remove");
        assert !declaredMethod(protectedItem, "setRemoved");
        assert !declaredMethod(protectedItem, "discard");
        long subscribedEvents = java.util.Arrays.stream(YuanDroppedWeaponProtection.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(net.minecraftforge.eventbus.api.SubscribeEvent.class)).count();
        assert subscribedEvents == 2 : "expiry and join replacement events must both be registered";
        assert java.util.Arrays.stream(YuanDroppedWeaponProtection.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(net.minecraftforge.eventbus.api.SubscribeEvent.class))
                .anyMatch(method -> method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0] == net.minecraftforge.event.entity.EntityJoinLevelEvent.class);
        String bindingSource = readSource("src/main/java/com/yuan/item/YuanWeaponBinding.java");
        assert bindingSource.contains("YuanWeaponDisabled") : "invalid duplicate must retain a private disabled marker";
        assert bindingSource.contains("isDisabled") : "disabled stacks must be rejected before identity assignment";
        assert bindingSource.contains("canUseWeapon(player") || bindingSource.contains("canUseWeapon(ServerPlayer")
                : "authoritative weapon API must be exposed";
        String dropSource = readSource("src/main/java/com/yuan/event/YuanDroppedWeaponProtection.java");
        assert dropSource.contains("EntityJoinLevelEvent");
        assert dropSource.contains("event.setCanceled(true)");
        assert dropSource.contains("addFreshEntity") : "vanilla reloaded drops must be replaced safely";
        com.yuan.event.YuanDroppedWeaponProtectionCheck.run();

        YuanDefenseState.updateSession(session, true, 10, 20, 1);
        YuanDefenseState.reset(session);
        assert Float.isNaN(YuanDefenseState.healthBaseline(session));
    }

    private static UUID weaponId(CompoundTag tag) {
        UUID id = YuanWeaponBinding.weaponId(tag);
        assert id != null;
        return id;
    }

    private static boolean declaredMethod(Class<?> type, String name) {
        return java.util.Arrays.stream(type.getDeclaredMethods()).anyMatch(method -> method.getName().equals(name));
    }

    private static String readSource(String path) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(path));
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }


    private static CompoundTag savedBinding(UUID owner, UUID weapon, long lastSeen,
                                            long missingSince, long lifecycleUntil) {
        CompoundTag stack = new CompoundTag();
        stack.putUUID(YuanWeaponBinding.WEAPON_UUID_TAG, weapon);
        stack.putUUID(YuanWeaponBinding.OWNER_UUID_TAG, owner);
        CompoundTag saved = new CompoundTag();
        saved.put("Stack", stack);
        saved.putLong("LastSeen", lastSeen);
        saved.putLong("MissingSince", missingSince);
        saved.putLong("LifecycleUntil", lifecycleUntil);
        CompoundTag playerData = new CompoundTag();
        CompoundTag persisted = new CompoundTag();
        persisted.put("YuanWeaponBinding", saved);
        playerData.put(Player.PERSISTED_NBT_TAG, persisted);
        return playerData;
    }
}
