package me.mytheria.hoppers.hopper;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;

public class HopperData {

    private int speedLevel = 0;
    private int rangeLevel = 0;
    private int lastTransferTick = 0;
    private boolean filterEnabled = false;
    private boolean filterUnlocked = false;
    private int unlockedFilterSlots = 0;
    private Material selectedFilterItem = null;
    private final Set<Material> filteredMaterials = new HashSet<>();

    public int getSpeedLevel() {
        return speedLevel;
    }

    public void setSpeedLevel(int speedLevel) {
        this.speedLevel = speedLevel;
    }

    public int getRangeLevel() {
        return rangeLevel;
    }

    public void setRangeLevel(int rangeLevel) {
        this.rangeLevel = rangeLevel;
    }

    public int getLastTransferTick() {
        return lastTransferTick;
    }

    public void setLastTransferTick(int lastTransferTick) {
        this.lastTransferTick = lastTransferTick;
    }

    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public void setFilterEnabled(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public Set<Material> getFilteredMaterials() {
        return filteredMaterials;
    }

    public boolean isAllowedByFilter(Material material) {
        if (!filterEnabled) {
            return true;
        }
        return filteredMaterials.contains(material);
    }

    public void addFilterMaterial(Material material) {
        filteredMaterials.add(material);
        selectedFilterItem = filteredMaterials.size() == 1 ? material : null;
    }

    public void removeFilterMaterial(Material material) {
        filteredMaterials.remove(material);
        selectedFilterItem = filteredMaterials.size() == 1 ? filteredMaterials.iterator().next() : null;
    }

    public boolean isFilterUnlocked() {
        return filterUnlocked;
    }

    public void setFilterUnlocked(boolean filterUnlocked) {
        this.filterUnlocked = filterUnlocked;
    }

    public int getUnlockedFilterSlots() {
        return unlockedFilterSlots;
    }

    public void setUnlockedFilterSlots(int unlockedFilterSlots) {
        this.unlockedFilterSlots = unlockedFilterSlots;
    }

    public Material getSelectedFilterItem() {
        return selectedFilterItem;
    }

    public void setSelectedFilterItem(Material selectedFilterItem) {
        this.selectedFilterItem = selectedFilterItem;
        if (selectedFilterItem != null) {
            filteredMaterials.clear();
            filteredMaterials.add(selectedFilterItem);
            this.filterEnabled = true;
        } else {
            filteredMaterials.clear();
            this.filterEnabled = false;
        }
    }

    public void setFilteredMaterials(Set<Material> materials) {
        filteredMaterials.clear();
        if (materials != null) filteredMaterials.addAll(materials);
        selectedFilterItem = filteredMaterials.size() == 1 ? filteredMaterials.iterator().next() : null;
        filterEnabled = !filteredMaterials.isEmpty();
    }
}
