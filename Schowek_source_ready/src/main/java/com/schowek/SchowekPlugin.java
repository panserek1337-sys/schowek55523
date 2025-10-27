package com.schowek;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SchowekPlugin extends JavaPlugin implements Listener {

    private String guiTitle;
    private List<String> itemKeys = new ArrayList<>();
    private Map<String, ItemDef> items = new LinkedHashMap<>();
    private Map<UUID, Map<String, Integer>> inMemoryCounts = new HashMap<>();

    // dobierz wszystko definition
    private DobierzDef dobierzDef = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigData();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SchowekPlugin włączony (bez zapisu, reset przy restarcie).");
    }

    @Override
    public void onDisable() {
        getLogger().info("SchowekPlugin wyłączony.");
    }

    private void loadConfigData() {
        FileConfiguration cfg = getConfig();
        this.guiTitle = ChatColor.translateAlternateColorCodes('&', cfg.getString("tytul", "&5Schowek"));

        ConfigurationSection sec = cfg.getConfigurationSection("przedmioty");
        itemKeys.clear();
        items.clear();
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection it = sec.getConfigurationSection(key);
                if (it == null) continue;
                String matName = it.getString("material", "STONE");
                String display = it.getString("nazwa", key);
                try {
                    Material mat = Material.valueOf(matName.toUpperCase());
                    ItemDef def = new ItemDef(mat, ChatColor.translateAlternateColorCodes('&', display),
                            it.getInt("limit", cfg.getInt("limity." + key, 5)),
                            it.getInt("ilosc", cfg.getInt("ilosci." + key, 1)));
                    itemKeys.add(key);
                    items.put(key, def);
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Nieprawidłowy materiał dla '" + key + "': " + matName + " - pomijam.");
                }
            }
        }

        ConfigurationSection d = cfg.getConfigurationSection("dobierz_wszystko");
        if (d != null) {
            String matName = d.getString("material", "EMERALD");
            String nazwa = d.getString("nazwa", "&aDobierz wszystko");
            int slot = d.getInt("slot", -1);
            try {
                Material mat = Material.valueOf(matName.toUpperCase());
                dobierzDef = new DobierzDef(mat, ChatColor.translateAlternateColorCodes('&', nazwa), slot);
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Nieprawidłowy materiał dla 'dobierz_wszystko': " + matName + " - przycisk wyłączony.");
                dobierzDef = null;
            }
        } else {
            dobierzDef = null;
        }
    }

    private static class ItemDef {
        public final Material material;
        public final String displayName;
        public final int limit;
        public final int amount;
        public ItemDef(Material material, String displayName, int limit, int amount) {
            this.material = material;
            this.displayName = displayName;
            this.limit = limit;
            this.amount = amount;
        }
    }

    private static class DobierzDef {
        public final Material material;
        public final String displayName;
        public final int slot;
        public DobierzDef(Material material, String displayName, int slot) {
            this.material = material;
            this.displayName = displayName;
            this.slot = slot;
        }
    }

    public void openSchowek(Player p) {
        int size = Math.max(9, ((itemKeys.size() + 8) / 9) * 9);
        // ensure there's at least one extra row if dobierz button needs slot beyond items
        int configuredSlot = (dobierzDef != null) ? dobierzDef.slot : -1;
        if (configuredSlot >= size) {
            size = Math.max(size, ((configuredSlot + 9) / 9) * 9);
        }
        Inventory inv = Bukkit.createInventory(null, size, guiTitle);

        for (int i = 0; i < itemKeys.size(); i++) {
            String key = itemKeys.get(i);
            ItemStack is = createItemStackFor(p, key);
            inv.setItem(i, is);
        }

        if (dobierzDef != null) {
            int slot = dobierzDef.slot;
            if (slot < 0 || slot >= size) slot = size - 1;
            ItemStack b = new ItemStack(dobierzDef.material, 1);
            ItemMeta m = b.getItemMeta();
            m.setDisplayName(dobierzDef.displayName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Kliknij, aby dobrać wszystko do limitu (tylko jeśli masz miejsce).");
            m.setLore(lore);
            b.setItemMeta(m);
            inv.setItem(slot, b);
        }

        p.openInventory(inv);
    }

    private ItemStack createItemStackFor(Player p, String key) {
        ItemDef def = items.get(key);
        ItemStack it = new ItemStack(def.material, 1);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(def.displayName);
        List<String> lore = new ArrayList<>();
        int remaining = getRemaining(p.getUniqueId(), key);
        lore.add(ChatColor.GRAY + "Pozostało refilli: " + ChatColor.YELLOW + remaining);
        lore.add("");
        lore.add(ChatColor.GREEN + "Kliknij, aby otrzymać refill.");
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private int getRemaining(UUID uuid, String key) {
        Map<String, Integer> map = inMemoryCounts.get(uuid);
        if (map == null) {
            map = new HashMap<>();
            for (String k : items.keySet()) {
                map.put(k, items.get(k).limit);
            }
            inMemoryCounts.put(uuid, map);
        }
        return map.getOrDefault(key, items.get(key).limit);
    }

    private void setRemaining(UUID uuid, String key, int amount) {
        Map<String, Integer> map = inMemoryCounts.get(uuid);
        if (map == null) {
            map = new HashMap<>();
            inMemoryCounts.put(uuid, map);
        }
        map.put(key, amount);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory() == null) return;
        if (!e.getInventory().getTitle().equals(guiTitle)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        int itemsCount = itemKeys.size();

        // dobierz button handling
        if (dobierzDef != null) {
            int dslot = dobierzDef.slot;
            int size = e.getInventory().getSize();
            if (dslot < 0 || dslot >= size) dslot = size - 1;
            if (slot == dslot) {
                handleDobierzWszystko(p);
                return;
            }
        }

        if (slot < 0 || slot >= itemsCount) return;
        String key = itemKeys.get(slot);
        if (key == null) return;

        int remaining = getRemaining(p.getUniqueId(), key);
        if (remaining <= 0) {
            p.sendMessage(ChatColor.RED + "Brak refilli dla: " + items.get(key).displayName + ". Poproś administrację o doładowanie.");
            p.closeInventory();
            return;
        }

        ItemDef def = items.get(key);
        int amount = def.amount;
        // attempt to give, but ensure there's space
        if (!hasSpaceFor(p.getInventory(), def.material, amount)) {
            p.sendMessage(ChatColor.RED + "Nie masz miejsca w ekwipunku!");
            p.closeInventory();
            return;
        }

        p.getInventory().addItem(new ItemStack(def.material, amount));
        setRemaining(p.getUniqueId(), key, remaining - 1);
        p.sendMessage(ChatColor.GREEN + "Dostałeś " + amount + " " + def.displayName + "!");
        openSchowek(p);
    }

    private void handleDobierzWszystko(Player p) {
        // compute total needed stacks and check space
        PlayerInventory inv = p.getInventory();
        int emptySlots = countEmptySlots(inv);
        int neededStacks = 0;
        Map<String, Integer> giveAmounts = new LinkedHashMap<>();
        for (String key : itemKeys) {
            ItemDef def = items.get(key);
            int remaining = getRemaining(p.getUniqueId(), key);
            if (remaining <= 0) continue;
            int amount = def.amount;
            // we treat we will give exactly 'amount' per refill use (not multiple uses)
            // Calculate how many units we will attempt to add: amount
            int remainToPlace = amount;
            // fill into existing partial stacks
            for (ItemStack is : inv.getContents()) {
                if (is == null) continue;
                if (is.getType() == def.material && is.getAmount() < def.material.getMaxStackSize()) {
                    int can = def.material.getMaxStackSize() - is.getAmount();
                    int take = Math.min(can, remainToPlace);
                    remainToPlace -= take;
                    if (remainToPlace <= 0) break;
                }
            }
            if (remainToPlace > 0) {
                int stacks = (remainToPlace + def.material.getMaxStackSize() - 1) / def.material.getMaxStackSize();
                neededStacks += stacks;
            }
            giveAmounts.put(key, amount);
        }

        if (neededStacks > emptySlots) {
            p.sendMessage(ChatColor.RED + "Nie masz miejsca w ekwipunku, aby dobrać wszystko!");
            p.closeInventory();
            return;
        }

        // All good: give items and decrement counts
        for (Map.Entry<String,Integer> e : giveAmounts.entrySet()) {
            String key = e.getKey();
            ItemDef def = items.get(key);
            p.getInventory().addItem(new ItemStack(def.material, def.amount));
            int remaining = getRemaining(p.getUniqueId(), key);
            setRemaining(p.getUniqueId(), key, Math.max(0, remaining - 1));
        }
        p.sendMessage(ChatColor.GREEN + "Dobrano wszystko do limitu.");
        openSchowek(p);
    }

    private int countEmptySlots(PlayerInventory inv) {
        int c = 0;
        for (ItemStack is : inv.getContents()) {
            if (is == null) c++;
        }
        return c;
    }

    private boolean hasSpaceFor(PlayerInventory inv, Material mat, int amount) {
        int remain = amount;
        // fill partial stacks
        for (ItemStack is : inv.getContents()) {
            if (is == null) continue;
            if (is.getType() == mat && is.getAmount() < mat.getMaxStackSize()) {
                int can = mat.getMaxStackSize() - is.getAmount();
                int take = Math.min(can, remain);
                remain -= take;
                if (remain <= 0) return true;
            }
        }
        // count empty slots needed
        int empty = 0;
        for (ItemStack is : inv.getContents()) {
            if (is == null) empty++;
        }
        int stacksNeeded = (remain + mat.getMaxStackSize() - 1) / mat.getMaxStackSize();
        return stacksNeeded <= empty;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("schowek")) return false;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Tylko gracze mogą otworzyć schowek.");
                return true;
            }
            Player p = (Player) sender;
            if (!sender.hasPermission("schowek.uzyj")) {
                sender.sendMessage(ChatColor.RED + "Brak uprawnień.");
                return true;
            }
            openSchowek(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("set")) {
            if (!sender.hasPermission("schowek.admin")) {
                sender.sendMessage(ChatColor.RED + "Brak uprawnień.");
                return true;
            }
            if (args.length != 4) {
                sender.sendMessage(ChatColor.YELLOW + "Użycie: /schowek set <gracz> <typ> <ilość>");
                return true;
            }
            String target = args[1];
            String typ = args[2].toLowerCase();
            int amt;
            try { amt = Integer.parseInt(args[3]); } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Ilość musi być liczbą.");
                return true;
            }
            if (!items.containsKey(typ)) {
                sender.sendMessage(ChatColor.RED + "Nieznany typ: " + typ);
                return true;
            }
            UUID uuid = Bukkit.getOfflinePlayer(target).getUniqueId();
            setRemaining(uuid, typ, amt);
            sender.sendMessage(ChatColor.GREEN + "Ustawiono " + amt + " refilli " + items.get(typ).displayName + " dla " + target);
            return true;
        } else if (sub.equals("giveall")) {
            if (!sender.hasPermission("schowek.admin")) {
                sender.sendMessage(ChatColor.RED + "Brak uprawnień.");
                return true;
            }
            if (args.length != 3) {
                sender.sendMessage(ChatColor.YELLOW + "Użycie: /schowek giveall <typ> <ilość>");
                return true;
            }
            String typ = args[1].toLowerCase();
            int amt;
            try { amt = Integer.parseInt(args[2]); } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Ilość musi być liczbą.");
                return true;
            }
            if (!items.containsKey(typ)) {
                sender.sendMessage(ChatColor.RED + "Nieznany typ: " + typ);
                return true;
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                int cur = getRemaining(p.getUniqueId(), typ);
                setRemaining(p.getUniqueId(), typ, cur + amt);
            }
            sender.sendMessage(ChatColor.GREEN + "Dodano " + amt + " refilli " + items.get(typ).displayName + " wszystkim graczom online.");
            return true;
        } else if (sub.equals("reload")) {
            if (!sender.hasPermission("schowek.admin")) {
                sender.sendMessage(ChatColor.RED + "Brak uprawnień.");
                return true;
            }
            reloadConfig();
            loadConfigData();
            inMemoryCounts.clear();
            sender.sendMessage(ChatColor.GREEN + "Schowek: config przeładowany.");
            return true;
        }

        return false;
    }
}
