-- GENERADO por ui/dev/SembrarTraducciones. No editar a mano.
--
-- Precarga la cache de traducciones con las versiones inglesas escritas a
-- mano del contenido de ejemplo. La clave es el SHA-256 del texto original,
-- que es la razon de que este fichero se genere en lugar de teclearse.
--
-- Idempotente por el WHERE NOT EXISTS de siempre: ejecutarlo en cada arranque
-- no duplica nada, y respeta cualquier traduccion ya guardada.

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '4b284e59d135cdc4dda14ae731f7aa8818db522536d8e7b61a228a9b8c5d32f1', 'en', 'A restored stone house halfway up the hillside, with a fireplace, a vegetable garden and open views of the mountains. The village is a ten-minute walk away.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '4b284e59d135cdc4dda14ae731f7aa8818db522536d8e7b61a228a9b8c5d32f1' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'ecf2ff8772b7d99a105957dbd43acfec5f01e8d558e68673327e4e87d1b7e0be', 'en', 'Second line from the beach, with a south-facing terrace and a lift. The bars and the seafront promenade are just around the corner.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'ecf2ff8772b7d99a105957dbd43acfec5f01e8d558e68673327e4e87d1b7e0be' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '3f0a2556600c1a56524429ffe2a2b80ba425225209526894902fb38fb5391ae6', 'en', 'A whitewashed villa with a private pool, a shaded porch and direct access to a small cove. Full board included.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '3f0a2556600c1a56524429ffe2a2b80ba425225209526894902fb38fb5391ae6' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'c0fe686e807d7efb6baceaf705912066aa790959ee7cacb4e19cf949f6ab962f', 'en', 'A wooden cabin for two, with a wood-burning stove and a picture window onto the beech forest. No phone signal and no neighbours: that is the whole point.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'c0fe686e807d7efb6baceaf705912066aa790959ee7cacb4e19cf949f6ab962f' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'ae374a5e6a4aa5ca9532722b97e1e2a80427ae438ad28ae335fcc8fdbf19c8f7', 'en', 'An open-plan loft in a 19th-century building, with exposed beams and four-metre ceilings. Right in the old town, walking distance from everything.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'ae374a5e6a4aa5ca9532722b97e1e2a80427ae438ad28ae335fcc8fdbf19c8f7' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'f1b7b9df15350d72b4d89df04be41baaa07d70d7c097131889ffd4b4473a227a', 'en', 'A townhouse with a patio, a barbecue and a shared pool, on a quiet development fifteen minutes from Las Canteras beach.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'f1b7b9df15350d72b4d89df04be41baaa07d70d7c097131889ffd4b4473a227a' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '630e872de77167331a99c88c4f9ef86e110bfebe42bf67412722c61e33b0a60c', 'en', 'A newly refurbished top-floor flat with a terrace, an open kitchen and air conditioning in both bedrooms. The whole neighbourhood is walkable: bars, theatres, and the Retiro fifteen minutes away.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '630e872de77167331a99c88c4f9ef86e110bfebe42bf67412722c61e33b0a60c' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '51d1dc603d134cd14deeee3385ca479b2d057b2eb601db4435a6000732fd44ce', 'en', 'A contemporary villa with an infinity pool, a panoramic terrace and sea views from both floors. Full board included.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '51d1dc603d134cd14deeee3385ca479b2d057b2eb601db4435a6000732fd44ce' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '6eabc8160883f017ec1b79fea1ba8968664658d1056bcd3e4e5cc0addab76573', 'en', 'A house with an inner courtyard in the heart of the old town, whitewashed walls and terracotta floors. The Tagus is a five-minute walk from the door.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '6eabc8160883f017ec1b79fea1ba8968664658d1056bcd3e4e5cc0addab76573' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '152dfacece8b9ac1cfb9dc16240d4813e498222530cf9dd894e783b27e383028', 'en', 'A wooden lodge with a wood-burning stove and views of the slopes from the porch. Private parking right by the door, which matters when it snows.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '152dfacece8b9ac1cfb9dc16240d4813e498222530cf9dd894e783b27e383028' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'd4ac03a708bcb1f93f6ad463d9ebaf8e26e85e62c5703f263444ec0e7d0380a8', 'en', 'Quiet, and a good breakfast'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'd4ac03a708bcb1f93f6ad463d9ebaf8e26e85e62c5703f263444ec0e7d0380a8' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'c992cb3ebd0015b416016ac8be1d1a8ea3837c8638a490747d0ee713a4710760', 'en', 'Perfect for switching off'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'c992cb3ebd0015b416016ac8be1d1a8ea3837c8638a490747d0ee713a4710760' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'f67a50677f98590587d8d2e6fae6a5d6a8ed88abca2bb6f6161a3ce3799422f2', 'en', 'You could not ask for more'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'f67a50677f98590587d8d2e6fae6a5d6a8ed88abca2bb6f6161a3ce3799422f2' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '55878a70d65fbff6357b58be05f95d422e882c1ef6fc27c14f163d4b7579256b', 'en', 'Very well located'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '55878a70d65fbff6357b58be05f95d422e882c1ef6fc27c14f163d4b7579256b' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'e9634286d765839035e74d6576b3183f75cf7f016c72bf21313c2609dda7faa3', 'en', 'The private cove is worth it'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'e9634286d765839035e74d6576b3183f75cf7f016c72bf21313c2609dda7faa3' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '495a72594ac651a11ad6340e219707302e94d3ca1b60de714601b25410b087c0', 'en', 'Pricey, but you enjoy every minute'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '495a72594ac651a11ad6340e219707302e94d3ca1b60de714601b25410b087c0' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'bc25023a4be473917ec34e0f52c1656702930686eb2cbf9fb6932dd41562e234', 'en', 'Genuinely rustic'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'bc25023a4be473917ec34e0f52c1656702930686eb2cbf9fb6932dd41562e234' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '9a0b9adf5ea42435b9bf03139f80f411dcea9a68718d39e2372e3763978fce7f', 'en', 'We could have used more heating'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '9a0b9adf5ea42435b9bf03139f80f411dcea9a68718d39e2372e3763978fce7f' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '527e33098b9c2c74e9e5c14813650afc8ad619f629845658c4b38beb34b153cf', 'en', 'The best spot in the neighbourhood'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '527e33098b9c2c74e9e5c14813650afc8ad619f629845658c4b38beb34b153cf' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '2a7be53f12c2ca69caefb0e55bdc28f79274f7622dc13d01087982cdf7a098bb', 'en', 'All the charm of an old building'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '2a7be53f12c2ca69caefb0e55bdc28f79274f7622dc13d01087982cdf7a098bb' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '76897d8d1573f3cb7d95e4a3afced3ffa3b768b5aca9ac3ca3c514a21f49c423', 'en', 'A good family trip'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '76897d8d1573f3cb7d95e4a3afced3ffa3b768b5aca9ac3ca3c514a21f49c423' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '650f1ef328dce6354019bf016f4e675cefebabb0cd24441c29766b011371e1f8', 'en', 'Solid from start to finish'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '650f1ef328dce6354019bf016f4e675cefebabb0cd24441c29766b011371e1f8' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'b4f16d50b2c54631dffdd861f3b8c3a630b88f9a0fafdf7f8ca41cb459c0d066', 'en', 'Unbeatable location'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'b4f16d50b2c54631dffdd861f3b8c3a630b88f9a0fafdf7f8ca41cb459c0d066' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '0416309176ffde98caaeb795c253be436b82dcae2ba52099fdefe125dbee04bf', 'en', 'Small, but very well thought out'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '0416309176ffde98caaeb795c253be436b82dcae2ba52099fdefe125dbee04bf' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'afd944648df3bbf97ef90322514aa56c05a02e8e4313a570a9106c1930f19a82', 'en', 'Worth every euro'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'afd944648df3bbf97ef90322514aa56c05a02e8e4313a570a9106c1930f19a82' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '9d22a2dc9e63774481e56f2a4f21db7e070cee4660d6392f274a698c87b11d40', 'en', 'Luxury without the showing off'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '9d22a2dc9e63774481e56f2a4f21db7e070cee4660d6392f274a698c87b11d40' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '26aaffc227693beef1e106c742c81d88d948ddeb3a9879b9e181531e51b4b841', 'en', 'The courtyard is another world'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '26aaffc227693beef1e106c742c81d88d948ddeb3a9879b9e181531e51b4b841' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '90a06faad51b9475f1523caed69c27bd0c8df37ccd469872ebc35960e58f525b', 'en', 'The real thing'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '90a06faad51b9475f1523caed69c27bd0c8df37ccd469872ebc35960e58f525b' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '7bb48fbdc1b143cdc643cdfa41d89ed7ae915a6ebbc4f1ff29b3dd81a4b1edb9', 'en', 'Complete disconnection'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '7bb48fbdc1b143cdc643cdfa41d89ed7ae915a6ebbc4f1ff29b3dd81a4b1edb9' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'abe4d9e634d5bc51c8aa8643e1ad081e18dd3e9bdeab213940164de84e92f917', 'en', 'Ideal for a ski trip'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'abe4d9e634d5bc51c8aa8643e1ad081e18dd3e9bdeab213940164de84e92f917' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'fd67f143c04b01e374cfd898e2ae7b569a0c8c4e0f6ae27421a2eb91f561e4dd', 'en', 'a country house for August'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'fd67f143c04b01e374cfd898e2ae7b569a0c8c4e0f6ae27421a2eb91f561e4dd' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'fdd44215fd49478afe7abb2e826fd8485faeb7fdcacb8ebb896eeb6d5e05b645', 'en', 'a city flat with good transport links'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'fdd44215fd49478afe7abb2e826fd8485faeb7fdcacb8ebb896eeb6d5e05b645' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'fbf9dfb88e48bafd1d2a23512981bf49b2303bee62fc65071e33d75510424871', 'en', 'a mountain cabin in winter'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'fbf9dfb88e48bafd1d2a23512981bf49b2303bee62fc65071e33d75510424871' AND targetLanguage = 'en');

