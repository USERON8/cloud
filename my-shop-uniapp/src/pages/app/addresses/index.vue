<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import AppShell from "../../../components/AppShell.vue";
import {
    addUserAddress,
    deleteUserAddress,
    listUserAddresses,
    updateUserAddress,
} from "../../../api/address";
import type { UserAddress } from "../../../types/domain";
import { sessionState } from "../../../auth/session";
import { confirm, toast } from "../../../utils/ui";

const rows = ref<UserAddress[]>([]);
const loading = ref(false);
const saving = ref(false);
const editingAddressId = ref<number | null>(null);
const userId = computed(() => sessionState.user?.id);

const form = reactive<UserAddress>({
    receiverName: "",
    receiverPhone: "",
    province: "",
    city: "",
    district: "",
    street: "",
    detailAddress: "",
    isDefault: 1,
});

function resetForm(): void {
    editingAddressId.value = null;
    form.receiverName = "";
    form.receiverPhone = "";
    form.province = "";
    form.city = "";
    form.district = "";
    form.street = "";
    form.detailAddress = "";
    form.isDefault = rows.value.length === 0 ? 1 : 0;
}

function toAddressPayload(address: UserAddress): UserAddress {
    return {
        receiverName: address.receiverName.trim(),
        receiverPhone: address.receiverPhone.trim(),
        province: address.province.trim(),
        city: address.city.trim(),
        district: address.district.trim(),
        street: address.street.trim(),
        detailAddress: address.detailAddress.trim(),
        isDefault: address.isDefault === 1 ? 1 : 0,
    };
}

function validateForm(): boolean {
    if (!form.receiverName.trim()) {
        toast("Consignee is required");
        return false;
    }
    if (!/^1[3-9]\d{9}$/.test(form.receiverPhone.trim())) {
        toast("Phone must use a mainland China mobile format");
        return false;
    }
    if (!form.province.trim() || !form.city.trim() || !form.district.trim()) {
        toast("Province, city, and district are required");
        return false;
    }
    if (!form.street.trim() || !form.detailAddress.trim()) {
        toast("Street and detail address are required");
        return false;
    }
    return true;
}

async function loadAddresses(): Promise<void> {
    if (loading.value) return;
    if (typeof userId.value !== "number") {
        toast("Missing user session");
        return;
    }
    loading.value = true;
    try {
        rows.value = await listUserAddresses(userId.value);
        if (
            editingAddressId.value == null &&
            rows.value.length > 0 &&
            form.receiverName.trim().length === 0
        ) {
            form.isDefault = 0;
        }
    } catch (error) {
        toast(
            error instanceof Error ? error.message : "Failed to load addresses",
        );
    } finally {
        loading.value = false;
    }
}

async function saveAddress(): Promise<void> {
    if (saving.value) {
        return;
    }
    if (typeof userId.value !== "number") {
        toast("Missing user session");
        return;
    }
    if (!validateForm()) {
        return;
    }

    saving.value = true;
    try {
        const payload = toAddressPayload(form);
        if (editingAddressId.value != null) {
            await updateUserAddress(editingAddressId.value, payload);
            toast("Address updated", "success");
        } else {
            await addUserAddress(userId.value, payload);
            toast("Address created", "success");
        }
        resetForm();
        await loadAddresses();
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to save the address",
        );
    } finally {
        saving.value = false;
    }
}

function startEdit(item: UserAddress): void {
    editingAddressId.value = typeof item.id === "number" ? item.id : null;
    form.receiverName = item.receiverName;
    form.receiverPhone = item.receiverPhone;
    form.province = item.province;
    form.city = item.city;
    form.district = item.district;
    form.street = item.street;
    form.detailAddress = item.detailAddress;
    form.isDefault = item.isDefault === 1 ? 1 : 0;
}

async function markAsDefault(item: UserAddress): Promise<void> {
    if (typeof item.id !== "number" || item.isDefault === 1) {
        return;
    }
    try {
        await updateUserAddress(item.id, {
            ...toAddressPayload(item),
            isDefault: 1,
        });
        toast("Default address updated", "success");
        await loadAddresses();
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to update the default address",
        );
    }
}

async function removeAddress(item: UserAddress): Promise<void> {
    if (typeof item.id !== "number") {
        return;
    }
    const ok = await confirm(`Delete the address for ${item.receiverName}?`);
    if (!ok) {
        return;
    }
    try {
        await deleteUserAddress(item.id);
        toast("Address deleted", "success");
        if (editingAddressId.value === item.id) {
            resetForm();
        }
        await loadAddresses();
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to delete the address",
        );
    }
}

resetForm();
onShow(() => {
    void loadAddresses();
});
</script>

<template>
    <AppShell title="Addresses">
        <view class="addresses-layout">
            <view class="hero-card dashboard-hero display-panel fade-in-up">
                <view class="hero-copy dashboard-hero-copy">
                    <text class="hero-eyebrow">Addresses</text>
                    <text class="hero-title"
                        >Create, edit, and organize delivery destinations in a
                        calmer address book.</text
                    >
                    <text class="hero-subtitle">
                        Keep a reliable default address ready so checkout stays
                        fast, clear, and consistent across future orders.
                    </text>
                </view>

                <view class="hero-stats dashboard-hero-stats">
                    <view class="info-card">
                        <text class="info-label">Saved addresses</text>
                        <text class="info-value">{{ rows.length }}</text>
                    </view>
                    <view class="info-card">
                        <text class="info-label">Default ready</text>
                        <text class="info-value">{{
                            rows.some((item) => item.isDefault === 1)
                                ? "Yes"
                                : "No"
                        }}</text>
                    </view>
                </view>
            </view>

            <view class="content-grid dashboard-grid-2">
                <view class="surface-card panel panel-block panel-hover editor-panel fade-in-up">
                    <view class="header">
                        <view class="section-block compact-block">
                            <text class="section-title">{{
                                editingAddressId
                                    ? "Edit address"
                                    : "New address"
                            }}</text>
                            <text class="section-subtitle"
                                >Capture recipient, region, street, and detailed
                                delivery information.</text
                            >
                        </view>
                        <button class="btn-outline" @click="resetForm">
                            Reset
                        </button>
                    </view>

                    <view class="form-grid">
                        <input
                            v-model="form.receiverName"
                            class="input field-control"
                            placeholder="Consignee"
                        />
                        <input
                            v-model="form.receiverPhone"
                            class="input field-control"
                            placeholder="Phone"
                        />
                        <input
                            v-model="form.province"
                            class="input field-control"
                            placeholder="Province"
                        />
                        <input
                            v-model="form.city"
                            class="input field-control"
                            placeholder="City"
                        />
                        <input
                            v-model="form.district"
                            class="input field-control"
                            placeholder="District"
                        />
                        <input
                            v-model="form.street"
                            class="input field-control"
                            placeholder="Street"
                        />
                    </view>

                    <textarea
                        v-model="form.detailAddress"
                        class="textarea field-control"
                        placeholder="Detail address"
                    />

                    <label class="default-row">
                        <switch
                            :checked="form.isDefault === 1"
                            @change="
                                form.isDefault = $event.detail.value ? 1 : 0
                            "
                        />
                        <text class="default-text">Set as default address</text>
                    </label>

                    <button
                        class="btn-primary submit-button"
                        :loading="saving"
                        @click="saveAddress"
                    >
                        {{
                            editingAddressId
                                ? "Update address"
                                : "Create address"
                        }}
                    </button>
                </view>

                <view class="surface-card panel panel-block panel-hover book-panel fade-in-up">
                    <view class="header">
                        <view class="section-block compact-block">
                            <text class="section-title">Address book</text>
                            <text class="section-subtitle"
                                >Choose the default destination or refine saved
                                delivery records.</text
                            >
                        </view>
                        <button class="btn-outline" @click="loadAddresses">
                            Refresh
                        </button>
                    </view>

                    <view v-if="rows.length === 0" class="empty-state"
                        >No address is available</view
                    >

                    <view v-else class="list">
                        <view
                            v-for="item in rows"
                            :key="item.id"
                            class="row-card surface-muted panel-hover"
                        >
                            <view class="row-head">
                                <view class="row-copy">
                                    <text class="name"
                                        >{{ item.receiverName }} |
                                        {{ item.receiverPhone }}</text
                                    >
                                    <text class="meta">
                                        {{ item.province }} {{ item.city }}
                                        {{ item.district }} {{ item.street }}
                                        {{ item.detailAddress }}
                                    </text>
                                </view>
                                <text
                                    v-if="item.isDefault === 1"
                                    class="status-chip default-chip"
                                    >Default</text
                                >
                            </view>

                            <view class="row-actions action-wrap">
                                <button
                                    class="btn-outline"
                                    @click="startEdit(item)"
                                >
                                    Edit
                                </button>
                                <button
                                    v-if="item.isDefault !== 1"
                                    class="btn-outline"
                                    @click="markAsDefault(item)"
                                >
                                    Set default
                                </button>
                                <button
                                    class="btn-secondary"
                                    @click="removeAddress(item)"
                                >
                                    Delete
                                </button>
                            </view>
                        </view>
                    </view>
                </view>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.addresses-layout {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.compact-block {
    gap: 6px;
}

.header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.textarea {
    min-height: 120px;
}

.default-row {
    display: flex;
    align-items: center;
    gap: 10px;
}

.default-text {
    font-size: 13px;
    color: var(--text-muted);
}

.submit-button {
    width: 100%;
}

.list {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.row-card {
    padding: 18px;
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.row-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
}

.row-copy {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.name {
    font-size: 16px;
    font-weight: 700;
    letter-spacing: -0.02em;
}

.meta {
    font-size: 13px;
    color: var(--text-muted);
    line-height: 1.7;
}

.default-chip {
    background: rgba(95, 209, 194, 0.16);
    color: var(--accent-strong);
}

.empty-state {
    padding: 28px 0;
    text-align: center;
    color: var(--text-muted);
    font-size: 13px;
}

@media (max-width: 900px) {
    .form-grid {
        grid-template-columns: 1fr;
    }

    .header {
        flex-direction: column;
    }
}
</style>
