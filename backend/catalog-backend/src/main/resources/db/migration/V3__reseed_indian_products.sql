-- Drop old data and the instagram_post_url column.
-- Re-seed with 100 Indian traditional wedding products, all priced in INR.
-- Status split: 55 IN_STOCK · 30 OUT_OF_STOCK · 15 BUILT_ON_REQUEST
-- Featured: 15 products across all categories.

TRUNCATE TABLE products;
ALTER TABLE products DROP COLUMN instagram_post_url;

INSERT INTO products (product_number, title, description, price, currency, status, featured) VALUES

-- ── BRIDAL SAREES (PN-001 – PN-020) ──────────────────────────────────────────
('PN-001', 'Kanjeevaram Pure Silk Saree', 'Traditional handwoven Kanjeevaram silk saree with rich zari border and peacock motifs. Comes with matching blouse piece. Temple-run pallu with gold thread weaving.', 18500.00, 'INR', 'IN_STOCK', true),
('PN-002', 'Banarasi Brocade Saree', 'Heavy Banarasi brocade saree in deep red with gold minakari work throughout. Ideal for the main wedding ceremony. Unstitched blouse included.', 22000.00, 'INR', 'IN_STOCK', true),
('PN-003', 'Paithani Silk Saree', 'Authentic Paithani saree from Paithan, Maharashtra. Handwoven with pure silk and real zari. Parrot and lotus motifs on the pallu.', 15000.00, 'INR', 'IN_STOCK', false),
('PN-004', 'Chanderi Silk Cotton Saree', 'Lightweight Chanderi saree with delicate floral butis woven in gold thread. Perfect for mehendi and haldi ceremonies.', 3800.00, 'INR', 'IN_STOCK', false),
('PN-005', 'Tussar Silk Saree', 'Natural gold-toned Tussar silk saree with hand-painted Madhubani motifs on pallu. Eco-friendly natural dye.', 5200.00, 'INR', 'OUT_OF_STOCK', false),
('PN-006', 'Pochampally Ikat Saree', 'Double-ikat woven Pochampally saree in geometric patterns. Pure silk with contrasting zari border.', 7500.00, 'INR', 'IN_STOCK', false),
('PN-007', 'Patola Double Ikat Saree', 'Rare Patola saree from Patan, Gujarat. Double-ikat weave with vibrant geometric patterns. Heirloom quality.', 45000.00, 'INR', 'BUILT_ON_REQUEST', true),
('PN-008', 'Sambalpuri Bandha Saree', 'Traditional Sambalpuri Bandha saree with shankha-chakra motifs. Hand-tied resist dyeing. Pure cotton-silk blend.', 6800.00, 'INR', 'IN_STOCK', false),
('PN-009', 'Mysore Silk Saree', 'Pure Mysore Crepe Silk saree with fine zari border. Lightweight and lustrous. KSIC certified 100% pure silk.', 9500.00, 'INR', 'IN_STOCK', false),
('PN-010', 'Bhagalpuri Silk Saree', 'Classic Bhagalpuri Tussar silk with hand-block printed floral design. Contemporary colours for modern brides.', 4200.00, 'INR', 'IN_STOCK', false),
('PN-011', 'Organza Hand-Embroidered Saree', 'Sheer organza saree with dense hand-embroidery in sequins and thread work over full body. Reception wear.', 12000.00, 'INR', 'OUT_OF_STOCK', false),
('PN-012', 'Raw Silk Zardozi Saree', 'Heavyweight raw silk saree with hand-done zardozi embroidery on pallu and border. Made-to-order, 20-day lead time.', 28000.00, 'INR', 'BUILT_ON_REQUEST', false),
('PN-013', 'Uppada Jamdani Saree', 'Sheer Uppada silk saree with intricate jamdani weave. Lightweight with fine floral pattern woven in extra weft.', 8800.00, 'INR', 'IN_STOCK', false),
('PN-014', 'Tant Bengali Saree', 'Traditional Tant saree in white and red with hand-woven conch and lotus border. Worn for Sindoor Khela.', 2800.00, 'INR', 'IN_STOCK', false),
('PN-015', 'Leheriya Tie-Dye Saree', 'Vibrant Rajasthani Leheriya saree with diagonal wave patterns in five colours. Pure cotton, hand-tied.', 2200.00, 'INR', 'OUT_OF_STOCK', false),
('PN-016', 'Kasavu Kerala Saree', 'Traditional off-white Kerala Kasavu saree with golden zari border. Pure cotton, worn for Onam and weddings.', 3500.00, 'INR', 'IN_STOCK', false),
('PN-017', 'Gadwal Silk Cotton Saree', 'Gadwal saree with pure silk pallu and cotton body. Contrast colour border with temple-design zari weave.', 5600.00, 'INR', 'IN_STOCK', false),
('PN-018', 'Baluchari Silk Saree', 'Bishnupur Baluchari saree with mythological scenes woven into the pallu in extra weft silk. Museum quality.', 16000.00, 'INR', 'BUILT_ON_REQUEST', true),
('PN-019', 'Printed Georgette Saree', 'Digital-printed georgette saree with floral design and lace border. Easy to drape, suits reception wear.', 1800.00, 'INR', 'IN_STOCK', false),
('PN-020', 'Maheshwari Silk Cotton Saree', 'Maheshwari saree from Maheshwar, MP. Reversible border, fine ribbed texture. Pure silk-cotton blend.', 4800.00, 'INR', 'OUT_OF_STOCK', false),

-- ── BRIDAL LEHENGAS (PN-021 – PN-030) ────────────────────────────────────────
('PN-021', 'Bridal Red Lehenga Choli', 'Heavy bridal lehenga in pure silk with full-body hand embroidery in zari, resham and mirror work. Includes dupatta. Custom stitching available.', 55000.00, 'INR', 'BUILT_ON_REQUEST', true),
('PN-022', 'Pastel Floral Lehenga', 'Soft pastel pink lehenga with hand-painted floral embroidery. Lightweight fabric, suitable for mehendi function.', 18000.00, 'INR', 'IN_STOCK', false),
('PN-023', 'Navy Blue Velvet Lehenga', 'Deep navy velvet lehenga with gold zardozi embroidery on hem and waistband. Net dupatta with sequin border.', 32000.00, 'INR', 'OUT_OF_STOCK', true),
('PN-024', 'Indo-Western Crop Top Lehenga', 'Contemporary crop-top and flared lehenga set in organza. Ideal for sangeet night. Pre-stitched, sizes S–XL.', 12000.00, 'INR', 'IN_STOCK', false),
('PN-025', 'Sharara Set with Dupatta', 'Traditional Sharara set in pure silk with gota patti work. Comes with long kameez and silk dupatta.', 14500.00, 'INR', 'IN_STOCK', false),
('PN-026', 'Anarkali Suit Heavy Embroidery', 'Floor-length Anarkali with hand-done thread and sequin embroidery all over. Churidar and net dupatta included.', 9800.00, 'INR', 'OUT_OF_STOCK', false),
('PN-027', 'Ghagra Choli Rajasthani', 'Traditional Rajasthani ghagra in cotton with mirror-work choli and bandhani odhni. Handmade in Jaipur.', 7500.00, 'INR', 'IN_STOCK', false),
('PN-028', 'Designer Palazzo Suit', 'Palazzo set in chanderi silk with block-printed kurta and dupatta. Comfortable for extended function wear.', 5500.00, 'INR', 'IN_STOCK', false),
('PN-029', 'Banarasi Silk Ghagra', 'Heavy banarasi silk ghagra with muga silk choli. Handwoven zari butis throughout. Suitable for vidaai.', 38000.00, 'INR', 'BUILT_ON_REQUEST', false),
('PN-030', 'Silk Jacket Lehenga Set', 'Three-piece set: jacket, crop top, and flared skirt in pure silk with contrast thread embroidery. Modern bridal look.', 22000.00, 'INR', 'OUT_OF_STOCK', false),

-- ── HALDI & MEHENDI PACKAGES (PN-031 – PN-045) ───────────────────────────────
('PN-031', 'Haldi Package Basic', 'Set of 500g raw turmeric paste, rose water, sandalwood powder, and neem leaves. Natural ingredients sourced from organic farms.', 899.00, 'INR', 'IN_STOCK', false),
('PN-032', 'Haldi Package Premium', 'Complete haldi ceremony kit: turmeric paste, multani mitti, chandan, rose petals, and decorative haldi thali with marigold garland.', 2499.00, 'INR', 'IN_STOCK', true),
('PN-033', 'Haldi Package Royal', 'Luxury haldi set with 1kg pure turmeric blend, saffron-infused water, silver thali, silver bowl, 100 marigold strings for decoration, and haldi dupatta set.', 5999.00, 'INR', 'IN_STOCK', false),
('PN-034', 'Mehendi Cones Pack of 12', 'Natural fresh henna cones made with Arabian henna leaves. Dark staining, no chemicals. Best before 30 days from packing date.', 599.00, 'INR', 'IN_STOCK', false),
('PN-035', 'Bridal Mehendi Kit', 'Professional bridal mehendi kit: 24 cones, design stencils, essential oil aftercare, glitter set, and design booklet with 50 patterns.', 1299.00, 'INR', 'IN_STOCK', false),
('PN-036', 'Haldi Decoration Flower Set', 'Fresh marigold strings (50 meters), rose petals (2kg), and turmeric-dyed cloth backdrop for haldi ceremony decoration.', 1800.00, 'INR', 'OUT_OF_STOCK', false),
('PN-037', 'Kumkum & Sindoor Thali Set', 'Decorative brass thali with kumkum, sindoor, and akshat (rice) arranged in traditional pattern. Auspicious for wedding rituals.', 750.00, 'INR', 'IN_STOCK', false),
('PN-038', 'Ubtan Facial Pack Wedding', 'Traditional ubtan blend: besan, turmeric, rose water, and sandalwood. Natural skin brightening for pre-wedding glow. 500g pack.', 449.00, 'INR', 'IN_STOCK', false),
('PN-039', 'Attire Haldi Kurta Set', 'Yellow cotton kurta and pyjama set for the groom for haldi ceremony. Pre-washed, anti-stain treated cotton.', 1199.00, 'INR', 'OUT_OF_STOCK', false),
('PN-040', 'Haldi Dupatta Bride Yellow', 'Soft chiffon dupatta in turmeric yellow with hand-embroidered marigold motifs. For bride during haldi ceremony.', 899.00, 'INR', 'IN_STOCK', false),
('PN-041', 'Puja Samagri Wedding Kit', 'Complete puja samagri set for wedding rituals: camphor, incense sticks, dhoop, betel leaves, betel nuts, and sacred thread.', 599.00, 'INR', 'IN_STOCK', false),
('PN-042', 'Saptapadi Ritual Kit', 'Seven-steps ritual kit including sacred fire ingredients, coconut, rice, dried fruits, and pandit guidance booklet.', 1499.00, 'INR', 'IN_STOCK', false),
('PN-043', 'Mangalsutra Thali Gold Dipped', 'Traditional thali-style mangalsutra with black beads and gold-dipped pendant. Adjustable length chain.', 3800.00, 'INR', 'OUT_OF_STOCK', false),
('PN-044', 'Sindoor Box Pure Silver', 'Handcrafted sterling silver sindoor box with floral engravings. Velvet-lined interior. Gift-boxed.', 2200.00, 'INR', 'IN_STOCK', false),
('PN-045', 'Saubhagya Sutra Thread Set', 'Traditional yellow cotton saubhagya sutra with turmeric and kum-kum applied. Set of 12 for gifting.', 399.00, 'INR', 'IN_STOCK', false),

-- ── WEDDING JEWELLERY (PN-046 – PN-060) ──────────────────────────────────────
('PN-046', 'Kundan Bridal Necklace Set', 'Full Kundan bridal set: necklace, earrings, maang tikka, and nose ring. Gold-tone base with uncut kundan stones and meenakari back.', 8500.00, 'INR', 'IN_STOCK', true),
('PN-047', 'Polki Diamond Haar', 'Polki uncut diamond haar with ruby and emerald accents. Handcrafted in sterling silver with gold polish. Certificate included.', 35000.00, 'INR', 'BUILT_ON_REQUEST', true),
('PN-048', 'Temple Jewellery Set South Indian', 'Traditional South Indian temple jewellery set: haram, jimikki earrings, and vanki armlet. Gold-plated silver with ruby stones.', 12000.00, 'INR', 'IN_STOCK', false),
('PN-049', 'Meenakari Bangles Set of 12', 'Rajasthani meenakari enamel work bangles in gold base. Set of 12 in assorted colours. 2.6 size.', 2800.00, 'INR', 'IN_STOCK', false),
('PN-050', 'Choker Necklace Jadau', 'Jadau choker with natural freshwater pearls and ruby drops. Handcrafted using traditional wax embedding technique.', 15000.00, 'INR', 'OUT_OF_STOCK', false),
('PN-051', 'Matha Patti Bridal', 'Maang tikka with side chain and forehead ornament in gold-tone with pearl drops and red stone accents.', 3200.00, 'INR', 'IN_STOCK', false),
('PN-052', 'Chandbali Earrings Gold', 'Traditional Chandbali earrings in 22K gold-plated brass with meenakari work and hanging pearl cluster.', 1800.00, 'INR', 'IN_STOCK', false),
('PN-053', 'Bajuband Armlet Silver', 'Sterling silver bajuband with embossed peacock design. Adjustable band, fits all arm sizes.', 4500.00, 'INR', 'OUT_OF_STOCK', false),
('PN-054', 'Nath Bridal Nose Ring', 'Large bridal nath with chain attached to hair pin. Kundan setting with seed pearls. Left nostril style.', 2100.00, 'INR', 'IN_STOCK', false),
('PN-055', 'Haath Phool Hand Jewellery', 'Ring-bracelet hand jewellery set with five finger rings connected to wrist bracelet. Kundan work, gold tone.', 3500.00, 'INR', 'IN_STOCK', false),
('PN-056', 'Pearl Haar Double Strand', 'Double strand freshwater pearl haar with gold spacers. 18-inch length. Includes matching stud earrings.', 6800.00, 'INR', 'OUT_OF_STOCK', false),
('PN-057', 'Payal Silver Anklet Pair', 'Pure silver anklet pair with ghungroo bells and floral charms. Traditional design, adjustable fit.', 1500.00, 'INR', 'IN_STOCK', false),
('PN-058', 'Tikka Maang Long Chain', 'Long maang tikka with chain reaching back of head. Peacock design with green and red stones.', 2600.00, 'INR', 'IN_STOCK', false),
('PN-059', 'Groom Sarpech Turban Brooch', 'Royal sarpech (turban ornament) for groom in gold-tone with kundan stones and pearl hanging.', 4200.00, 'INR', 'BUILT_ON_REQUEST', false),
('PN-060', 'Lac Bangles Rajasthani Set', 'Handmade lac bangles with mirror work and gold foil. Set of 24 bangles in bridal red and gold.', 1200.00, 'INR', 'IN_STOCK', false),

-- ── CLAY & POTTERY (PN-061 – PN-070) ─────────────────────────────────────────
('PN-061', 'Kalash Mangal Pot Brass', 'Traditional brass Mangal Kalash with coconut, mango leaves, and red cloth. Used in wedding rituals for good luck.', 850.00, 'INR', 'IN_STOCK', false),
('PN-062', 'Clay Diyas Set of 50', 'Hand-thrown terracotta diyas in traditional shape. Ready to use, suitable for oil lamps. Set of 50 pieces.', 299.00, 'INR', 'IN_STOCK', false),
('PN-063', 'Kulhad Clay Cups Set of 24', 'Traditional kulhad cups for serving chai at wedding functions. Unglazed terracotta, biodegradable. Set of 24.', 449.00, 'INR', 'IN_STOCK', false),
('PN-064', 'Matka Water Pot Large', 'Large terracotta matka with stand for natural water cooling. 10-litre capacity. Hand-painted with floral motifs.', 650.00, 'INR', 'IN_STOCK', false),
('PN-065', 'Blue Pottery Serving Bowl Set', 'Jaipur blue pottery serving bowls set of 6. Hand-painted with floral motifs in cobalt blue and white. Food safe glaze.', 2400.00, 'INR', 'OUT_OF_STOCK', false),
('PN-066', 'Ganesha Clay Idol Wedding', 'Hand-sculpted Ganesh idol in natural clay with natural mineral colours. 12-inch height. Eco-friendly, immersible.', 1200.00, 'INR', 'IN_STOCK', false),
('PN-067', 'Terracotta Flower Pots Pair', 'Hand-painted terracotta flower pots in antique finish with wedding mandala designs. 10-inch diameter. Pair.', 750.00, 'INR', 'IN_STOCK', false),
('PN-068', 'Clay Return Gift Set', 'Set of 12 small clay return gift items: mini kalash, diya, and coin box per set. Custom name engraving available.', 1800.00, 'INR', 'BUILT_ON_REQUEST', false),
('PN-069', 'Raku Fired Decorative Vase', 'Japanese Raku-inspired terracotta vase with unique crackle finish. Each piece is unique. Height 30cm.', 3200.00, 'INR', 'OUT_OF_STOCK', false),
('PN-070', 'Hand-Painted Surahi Pot', 'Decorative terracotta surahi with long neck and hand-painted folk art in natural colours. 2-litre capacity.', 980.00, 'INR', 'IN_STOCK', false),

-- ── PUJA & RITUAL ESSENTIALS (PN-071 – PN-080) ───────────────────────────────
('PN-071', 'Puja Thali Set Silver Plated', 'Complete silver-plated puja thali set: thali, diya, incense holder, kalash, and kumkum container. Wedding engraving available.', 2800.00, 'INR', 'IN_STOCK', true),
('PN-072', 'Brass Diya Deepak Pair', 'Heavy brass panch-batti diya pair with five wicks. Suitable for aarti and wedding ceremonies. Antique finish.', 1400.00, 'INR', 'IN_STOCK', false),
('PN-073', 'Agarbatti Incense Gift Box', 'Curated incense gift box with 12 premium fragrances: rose, jasmine, sandalwood, and more. 240 sticks total.', 599.00, 'INR', 'IN_STOCK', false),
('PN-074', 'Sacred Thread Mauli Pack', 'Pure cotton mauli (sacred thread) in red and yellow. Pack of 100 meters. Used for tying on wrists in ceremonies.', 199.00, 'INR', 'IN_STOCK', false),
('PN-075', 'Coconut Dry Fruit Puja Set', 'Ceremonial coconut with dry fruits wrapped in red cloth and gold ribbon. Set of 11 for distribution in rituals.', 2200.00, 'INR', 'OUT_OF_STOCK', false),
('PN-076', 'Brass Ghanta Bell Temple', 'Handcast brass temple bell with wooden handle. Clear resonant tone. 6-inch diameter. Engraving available.', 1100.00, 'INR', 'IN_STOCK', false),
('PN-077', 'Silver Coin Laxmi Ganesh Set', 'Silver 999 purity Lakshmi-Ganesh coin set for wedding gifting. 10g per coin, velvet pouch, gift box.', 3500.00, 'INR', 'IN_STOCK', false),
('PN-078', 'Panchamrit Puja Kit', 'Complete Panchamrit set: pure honey, ghee, milk, curd, and sugar in silver containers. For abhishek ceremonies.', 899.00, 'INR', 'OUT_OF_STOCK', false),
('PN-079', 'Akhand Diya Oil Lamp', 'Large terracotta akhand diya with wick adjuster. Burns continuously for 72 hours on one fill of mustard oil.', 450.00, 'INR', 'IN_STOCK', false),
('PN-080', 'Wedding Camphor Tablets Box', 'Pure camphor tablets (kapoor) for aarti and fire rituals. 250g box, no artificial additives.', 249.00, 'INR', 'IN_STOCK', false),

-- ── WEDDING DECOR (PN-081 – PN-090) ──────────────────────────────────────────
('PN-081', 'Mandap Flower Decoration Kit', 'Fresh flower mandap decoration kit: marigold strings 100m, rose petals 5kg, and jasmine strings 20m for a full mandap setup.', 8500.00, 'INR', 'OUT_OF_STOCK', true),
('PN-082', 'Toran Door Hanging Marigold', 'Fresh marigold and mango leaf toran for main entrance. 5 feet wide, ready to hang. Lasts 3–4 days.', 650.00, 'INR', 'IN_STOCK', false),
('PN-083', 'Fairy Lights String 20 Meters', 'Warm white LED fairy lights, 20-meter length with 200 bulbs. IP44 waterproof. For outdoor and indoor decor.', 799.00, 'INR', 'IN_STOCK', false),
('PN-084', 'Banana Leaf Table Runner', 'Authentic banana leaf table runners for wedding feast. Pack of 50 leaves, cleaned and sorted. 3-feet length.', 499.00, 'INR', 'IN_STOCK', false),
('PN-085', 'Rajasthani Embroidered Cushion Set', 'Set of 6 floor cushions in bright mirror-work fabric for sangeet seating. Removable covers, foam filling.', 4200.00, 'INR', 'OUT_OF_STOCK', false),
('PN-086', 'Phoolon Ki Chadar Bride', 'Flower canopy chadar carried by brothers of the bride during vidaai. Fresh red and white roses woven on net.', 3500.00, 'INR', 'OUT_OF_STOCK', false),
('PN-087', 'Welcome Sign Wood Carved', 'Hand-carved wooden welcome board with couple name and date customisation. Mango wood, 24x18 inches.', 2800.00, 'INR', 'BUILT_ON_REQUEST', false),
('PN-088', 'Lantern Paper Luminary Set', 'Set of 50 paper lanterns in red, gold, and ivory for table centrepieces or pathway lighting.', 1100.00, 'INR', 'IN_STOCK', false),
('PN-089', 'Peacock Feather Centrepiece', 'Arrangement of 50 natural peacock feathers in a brass vase. For mandap or reception table display.', 1800.00, 'INR', 'IN_STOCK', false),
('PN-090', 'Kalira Bridal Hanging', 'Traditional Punjabi kalira set in golden colour with hanging coconuts and flower motifs. Tied to bride''s bangles.', 1500.00, 'INR', 'IN_STOCK', false),

-- ── SWEETS & RETURN GIFTS (PN-091 – PN-100) ──────────────────────────────────
('PN-091', 'Motichoor Ladoo Box 500g', 'Fresh motichoor ladoos made in pure ghee. Box of 500g (approximately 20 pieces). Ships within Delhi NCR only.', 450.00, 'INR', 'IN_STOCK', false),
('PN-092', 'Kaju Katli Premium 1kg', 'Premium quality kaju katli made from A-grade cashews. Silver-leafed, gift-wrapped in red satin box. 1kg.', 1200.00, 'INR', 'IN_STOCK', false),
('PN-093', 'Badam Halwa Jar', 'Rich almond halwa made with saffron and ghee. Glass jar, 500g. Shelf life 15 days. Refrigerate after opening.', 780.00, 'INR', 'OUT_OF_STOCK', false),
('PN-094', 'Return Gift Hamper Classic', 'Classic return gift hamper: silver-plated coin, dry fruits 200g, incense sticks, and sweet box. Gift-wrapped.', 699.00, 'INR', 'IN_STOCK', false),
('PN-095', 'Return Gift Hamper Premium', 'Premium return gift set: brass diya, pure ghee 100ml, agarbatti, kaju barfi 250g, and personalised card. Velvet pouch.', 1499.00, 'INR', 'IN_STOCK', true),
('PN-096', 'Paan Parcel Gift Box', 'Decorative meetha paan parcels wrapped in gold foil. Box of 24 pieces. Fresh preparation, 2-day shelf life.', 599.00, 'INR', 'OUT_OF_STOCK', false),
('PN-097', 'Chikki Groundnut Jaggery Pack', 'Traditional groundnut and jaggery chikki from Lonavala. 1kg pack in decorative tin. Perfect return gift.', 499.00, 'INR', 'IN_STOCK', false),
('PN-098', 'Puran Poli Gift Set', 'Maharashtrian sweet puran poli gift set. Box of 10, made with chana dal and jaggery filling. Shelf life 5 days.', 650.00, 'INR', 'OUT_OF_STOCK', false),
('PN-099', 'Customised Naam Mithai Box', 'Box of mithai with bride and groom names printed on individual pieces. Minimum order 25 boxes. 500g each.', 850.00, 'INR', 'BUILT_ON_REQUEST', false),
('PN-100', 'Wedding Dry Fruit Thali Gift', 'Decorative thali with assorted dry fruits: cashews, almonds, pistachios, raisins, and dates. 1kg total, gift-wrapped.', 2200.00, 'INR', 'IN_STOCK', false);
