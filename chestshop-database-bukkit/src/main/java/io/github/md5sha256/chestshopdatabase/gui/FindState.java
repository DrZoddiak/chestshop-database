package io.github.md5sha256.chestshopdatabase.gui;

import io.github.md5sha256.chestshopdatabase.model.ChestshopItem;
import io.github.md5sha256.chestshopdatabase.model.Shop;
import io.github.md5sha256.chestshopdatabase.model.ShopAttribute;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.util.BlockPosition;
import io.github.md5sha256.chestshopdatabase.util.SortDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class FindState {

    private static final int NUM_SHOP_TYPES = ShopType.values().length;

    private final EnumSet<ShopType> shopTypes = EnumSet.noneOf(ShopType.class);
    private final Map<ShopAttribute, ShopAttributeMeta> attributeMeta = new EnumMap<>(
            ShopAttribute.class);
    private boolean hideEmptyShops = false;
    private boolean hideFullShops = false;
    private boolean fuzzySearch = false;
    private final Map<ShopAttribute, Comparator<Shop>> comparators = new EnumMap<>(ShopAttribute.class);
    private final ChestshopItem item;
    private UUID world = null;
    private BlockPosition queryPosition = null;

    public FindState(
            @NotNull ChestshopItem item,
            @NotNull Map<ShopAttribute, Comparator<Shop>> comparators) {
        this.item = item;
        this.comparators.putAll(comparators);
        reset();
    }

    public FindState(@NotNull FindState other) {
        this.item = new ChestshopItem(other.item);
        this.world = other.world;
        this.queryPosition = other.queryPosition;
        this.shopTypes.addAll(other.shopTypes);
        for (Map.Entry<ShopAttribute, ShopAttributeMeta> entry : other.attributeMeta.entrySet()) {
            this.attributeMeta.put(entry.getKey(), new ShopAttributeMeta(entry.getValue()));
        }
        this.hideEmptyShops = other.hideEmptyShops;
        this.hideFullShops = other.hideFullShops;
        this.fuzzySearch = other.fuzzySearch;
        this.comparators.putAll(other.comparators);
    }

    public boolean showAllShopTypes() {
        return this.shopTypes.size() == NUM_SHOP_TYPES;
    }

    public ChestshopItem item() {
        return this.item;
    }

    public void clear() {
        this.attributeMeta.clear();
        this.shopTypes.clear();
    }

    public void reset() {
        setShopTypes(EnumSet.allOf(ShopType.class));
        this.attributeMeta.clear();
        for (ShopAttribute attribute : ShopAttribute.values()) {
            this.attributeMeta.put(attribute,
                    new ShopAttributeMeta(attribute, SortDirection.ASCENDING, 0));
        }
        this.world = null;
        this.queryPosition = null;
    }

    public void setWorld(@NotNull UUID world) {
        this.world = world;
    }
    public void setQueryPosition(@NotNull BlockPosition position) {
        this.queryPosition = position;
    }


    public Optional<UUID> world() {
        return Optional.ofNullable(this.world);
    }

    public @Nullable BlockPosition queryPosition() { return this.queryPosition; }

    public ShopAttributeMeta getOrCreate(@NotNull ShopAttribute attribute) {
        return this.attributeMeta.computeIfAbsent(attribute, ShopAttributeMeta::new);
    }

    public void clearShopAttributeMeta(@NotNull ShopAttribute attribute) {
        this.attributeMeta.remove(attribute);
    }

    public Set<ShopAttribute> selectedAttributes() {
        return Collections.unmodifiableSet(this.attributeMeta.keySet());
    }

    public void setShopTypes(@NotNull Collection<ShopType> shopTypes) {
        this.shopTypes.clear();
        this.shopTypes.addAll(shopTypes);
    }

    public void setSortDirection(@NotNull ShopAttribute shopAttribute,
                                 SortDirection sortDirection) {
        ShopAttributeMeta meta = this.attributeMeta.get(shopAttribute);
        if (meta != null) {
            meta.sortDirection(sortDirection);
        }
    }

    public void setHideEmptyShops(boolean hideEmptyShops) { this.hideEmptyShops = hideEmptyShops; }
    public void setHideFullShops(boolean hideFullShops) {  this.hideFullShops = hideFullShops; }

    public boolean fuzzySearch() { return this.fuzzySearch; }
    public void setFuzzySearch(boolean fuzzySearch) { this.fuzzySearch = fuzzySearch; }

    public void setSortPriority(@NotNull ShopAttribute shopAttribute, int priority) {
        ShopAttributeMeta meta = this.attributeMeta.get(shopAttribute);
        if (meta != null) {
            meta.weight(priority);
        }
    }

    @NotNull
    public Set<ShopType> shopTypes() {
        return Collections.unmodifiableSet(this.shopTypes);
    }

    @NotNull
    public Set<ShopAttribute> undeclaredAttributesForSorting() {
        return EnumSet.complementOf(EnumSet.copyOf(this.attributeMeta.keySet()));
    }

    @Nullable
    private Comparator<Shop> toComparator(@NotNull ShopAttributeMeta meta) {
        Comparator<Shop> base = this.comparators.get(meta.attribute());
        if (base == null) {
            return null;
        }
        if (meta.sortDirection() == SortDirection.ASCENDING) {
            return base;
        }
        return base.reversed();
    }

    @NotNull
    public Stream<Shop> applyToStream(@NotNull Stream<Shop> stream) {
        return applyShopTypeFilter(
                applySortingCharacteristics(
                        applyHideEmptyShops(
                                applyHideFullShops(stream))));
    }

    @NotNull
    protected Stream<Shop> applySortingCharacteristics(@NotNull Stream<Shop> stream) {

        Iterator<Comparator<Shop>> iterator = attributeMeta.values()
                .stream()
                .sorted(Comparator.<ShopAttributeMeta>comparingInt(ShopAttributeMeta::weight).reversed())
                .map(this::toComparator)
                .filter(Objects::nonNull)
                .iterator();
        if (!iterator.hasNext()) {
            // No comparators to sort
            return stream;
        }
        Comparator<Shop> comparator = iterator.next();
        while (iterator.hasNext()) {
            comparator = comparator.thenComparing(iterator.next());
        }
        return stream.sorted(comparator);
    }

    @NotNull
    protected Stream<Shop> applyShopTypeFilter(@NotNull Stream<Shop> stream) {
        if (this.shopTypes.isEmpty()) {
            return Stream.empty();
        } else if (this.shopTypes.size() < NUM_SHOP_TYPES) {
            return stream.filter(shop -> this.shopTypes.contains(shop.shopType()));
        }
        return stream;
    }

    @NotNull
    protected Stream<Shop> applyHideEmptyShops(@NotNull Stream<Shop> stream) {
        if (!this.hideEmptyShops) { return stream; }
        return stream.filter(shop -> shop.stock() > 0);
    }

    @NotNull
    protected Stream<Shop> applyHideFullShops(@NotNull Stream<Shop> stream) {
        if (!this.hideFullShops) { return stream; }
        return stream.filter(shop -> shop.estimatedCapacity() > 0);
    }
}
