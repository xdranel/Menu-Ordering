-- SQL Script to Add Menu Images from Unsplash
-- Run this script to add beautiful food images to your menu items
-- All images are from Unsplash (free to use)

USE restaurant_db;

-- PROMO Items
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=400&h=300&fit=crop' WHERE name = 'Paket Hemat 1';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=400&h=300&fit=crop' WHERE name = 'Paket Keluarga';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&h=300&fit=crop' WHERE name = 'Promo Akhir Tahun';

-- PAKET KOMBO
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=400&h=300&fit=crop' WHERE name = 'Combo A';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1586190848861-99aa4a171e90?w=400&h=300&fit=crop' WHERE name = 'Combo B';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1585238341710-4a9c77f4d3f7?w=400&h=300&fit=crop' WHERE name = 'Combo Family';

-- MAKANAN UTAMA
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1512058564366-18510be2db19?w=400&h=300&fit=crop' WHERE name = 'Nasi Goreng Spesial';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=400&h=300&fit=crop' WHERE name = 'Ayam Goreng Crispy';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&h=300&fit=crop' WHERE name = 'Burger Beef';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=400&h=300&fit=crop' WHERE name = 'Spaghetti Carbonara';

-- SOUP
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1547592166-23ac45744acd?w=400&h=300&fit=crop' WHERE name = 'Sop Ayam';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=400&h=300&fit=crop' WHERE name = 'Sop Iga';

-- MINUMAN
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=400&h=300&fit=crop' WHERE name = 'Es Teh Manis';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=400&h=300&fit=crop' WHERE name = 'Jus Jeruk';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=400&h=300&fit=crop' WHERE name = 'Kopi Hitam';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1548839140-29a749e1cf4d?w=400&h=300&fit=crop' WHERE name = 'Air Mineral';

-- SIDE DISH
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400&h=300&fit=crop' WHERE name = 'Kentang Goreng';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1639024471283-03518883512d?w=400&h=300&fit=crop' WHERE name = 'Onion Ring';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&h=300&fit=crop' WHERE name = 'Salad Sayur';

-- DESSERT
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=400&h=300&fit=crop' WHERE name = 'Ice Cream Vanilla';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1624353365286-3f8d62daad51?w=400&h=300&fit=crop' WHERE name = 'Pudding Coklat';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1524351199678-941a58a3df50?w=400&h=300&fit=crop' WHERE name = 'Cheese Cake';

-- SEASONAL
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=400&h=300&fit=crop' WHERE name = 'Menu Musiman Panas';
UPDATE menus SET image_url = 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=400&h=300&fit=crop' WHERE name = 'Menu Festival';

-- Verify the updates
SELECT id, name, image_url FROM menus ORDER BY category_id, id;
