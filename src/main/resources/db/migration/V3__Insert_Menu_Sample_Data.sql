-- Sample categories (Indonesian)
INSERT INTO categories (name, display_order)
VALUES ('Makanan Berat', 2),
       ('Cemilan', 3),
       ('Minuman Dingin', 4),
       ('Minuman Panas', 5),
       ('Dessert', 6);

-- Sample menus — Makanan Berat
INSERT INTO menus (name, description, price, available, is_promo, promo_price, category_id, created_by)
VALUES ('Nasi Goreng Spesial',
        'Nasi goreng dengan telur mata sapi, ayam suwir, kerupuk, dan acar',
        25000.00, TRUE, TRUE, 20000.00,
        (SELECT id FROM categories WHERE name = 'Makanan Berat'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Mie Goreng Spesial',
        'Mie goreng dengan bakso, sawi, dan telur orak-arik',
        22000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Makanan Berat'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Nasi Ayam Bakar',
        'Nasi putih dengan ayam bakar bumbu kecap, lalapan, dan sambal',
        32000.00, TRUE, TRUE, 28000.00,
        (SELECT id FROM categories WHERE name = 'Makanan Berat'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Nasi Rendang',
        'Nasi putih dengan rendang daging sapi empuk khas Padang',
        38000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Makanan Berat'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Nasi Soto Ayam',
        'Nasi putih dengan soto ayam bening, tauge, dan perkedel jagung',
        28000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Makanan Berat'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Nasi Uduk Komplit',
        'Nasi uduk gurih dengan ayam goreng, tempe orek, bihun, dan kerupuk',
        22000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Makanan Berat'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Nasi Goreng Seafood',
        'Nasi goreng dengan udang, cumi, dan sayuran',
        32000.00, TRUE, TRUE, 27000.00,
        (SELECT id FROM categories WHERE name = 'Makanan Berat'),
        (SELECT id FROM cashiers WHERE username = 'admin'));

-- Sample menus — Cemilan
INSERT INTO menus (name, description, price, available, is_promo, promo_price, category_id, created_by)
VALUES ('Pisang Goreng Keju',
        'Pisang goreng crispy dengan topping keju parut',
        12000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Cemilan'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Kentang Goreng',
        'Kentang goreng renyah dengan saus sambal dan mayonaise',
        15000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Cemilan'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Tempe Mendoan',
        'Tempe tipis digoreng dengan tepung bumbu, disajikan hangat',
        8000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Cemilan'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Tahu Crispy',
        'Tahu goreng crispy dengan isian sayuran, saus kacang',
        10000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Cemilan'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Bakwan Jagung',
        'Bakwan jagung manis goreng, renyah di luar lembut di dalam',
        8000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Cemilan'),
        (SELECT id FROM cashiers WHERE username = 'admin'));

-- Sample menus — Minuman Dingin
INSERT INTO menus (name, description, price, available, is_promo, promo_price, category_id, created_by)
VALUES ('Es Teh Manis',
        'Teh manis segar dengan es batu',
        8000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Minuman Dingin'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Es Jeruk Peras',
        'Jeruk segar diperas, manis dan menyegarkan',
        12000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Minuman Dingin'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Es Kopi Susu',
        'Kopi robusta dengan susu segar dan gula aren, disajikan dingin',
        20000.00, TRUE, TRUE, 15000.00,
        (SELECT id FROM categories WHERE name = 'Minuman Dingin'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Jus Alpukat',
        'Alpukat segar diblender dengan susu kental manis dan coklat',
        22000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Minuman Dingin'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Es Cincau Hijau',
        'Cincau hijau segar dengan santan dan gula merah',
        10000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Minuman Dingin'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Es Lemon Tea',
        'Teh dingin dengan perasan lemon segar',
        13000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Minuman Dingin'),
        (SELECT id FROM cashiers WHERE username = 'admin'));

-- Sample menus — Minuman Panas
INSERT INTO menus (name, description, price, available, is_promo, promo_price, category_id, created_by)
VALUES ('Teh Panas',
        'Teh hitam panas, manis atau tawar sesuai selera',
        6000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Minuman Panas'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Kopi Hitam Panas',
        'Kopi robusta hitam panas tanpa susu',
        10000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Minuman Panas'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Kopi Susu Panas',
        'Kopi robusta dengan susu segar panas, sedikit manis',
        15000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Minuman Panas'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Wedang Jahe',
        'Jahe merah segar dengan gula aren, menghangatkan badan',
        10000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Minuman Panas'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Susu Coklat Panas',
        'Susu segar panas dengan coklat bubuk premium',
        15000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Minuman Panas'),
        (SELECT id FROM cashiers WHERE username = 'admin'));

-- Sample menus — Dessert
INSERT INTO menus (name, description, price, available, is_promo, promo_price, category_id, created_by)
VALUES ('Es Krim 2 Scoop',
        'Pilihan rasa: coklat, vanila, atau stroberi',
        18000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Dessert'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Puding Coklat',
        'Puding coklat lembut dengan saus karamel',
        13000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Dessert'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Klepon (5 pcs)',
        'Klepon isi gula merah cair, dibalut kelapa parut',
        10000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Dessert'),
        (SELECT id FROM cashiers WHERE username = 'admin')),

       ('Bubur Sumsum',
        'Bubur sumsum lembut dengan kuah gula merah dan santan',
        12000.00, TRUE, FALSE, NULL,
        (SELECT id FROM categories WHERE name = 'Dessert'),
        (SELECT id FROM cashiers WHERE username = 'admin'));
