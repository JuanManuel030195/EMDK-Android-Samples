package com.symbol.barcodesample1;

import org.jetbrains.annotations.NotNull;

public class AssetPerValidation {
    private final int validationId;
    private final String assetNumber;
    private boolean scanned;
    private ValidationStatus status;

    public AssetPerValidation(int validationId, String assetNumber) {
        this.validationId = validationId;
        this.assetNumber = assetNumber;
        this.scanned = false;
        this.status = ValidationStatus.PENDING;
    }

    public int getValidationId() {
        return validationId;
    }

    public String getAssetNumber() {
        return assetNumber;
    }

    public boolean isScanned() {
        return scanned;
    }

    public void setScanned(boolean scanned) {
        this.scanned = scanned;
    }

    public ValidationStatus getStatus() {
        return status;
    }

    public void setStatus(
        @NotNull Asset asset,
        @NotNull Employee employee,
        @NotNull Building building
    ) {
        if (!asset.getEmployeeNumber().equals(employee.getNumber())) {
            this.status = ValidationStatus.WRONG_EMPLOYEE;
            return;
        }

        if (!asset.getBuildingName().equals(building.getName())) {
            this.status = ValidationStatus.WRONG_BUILDING;
            return;
        }

        this.status = ValidationStatus.OK;
    }

    public void setStatus(ValidationStatus status) {
        this.status = status;
    }
}
