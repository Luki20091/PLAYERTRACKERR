ItemsAdder pack: piracki_kompas

This folder is an example ItemsAdder pack adjusted for ItemsAdder 1.21.8.

Structure:
- configs/piracki_kompas.yml  (info.namespace: piracki_kompas, model_path: item)
 - resourcepack/assets/piracki_kompas/models/item/piracki_kompas.json (currently uses built-in compass texture as fallback)
 - resourcepack/assets/piracki_kompas/textures/item/piracki_kompas/ (place piracki_kompas_00.png ... piracki_kompas_31.png here to enable custom animation)

How to deploy on your server:
1. Copy the whole folder `plugins/ItemsAdder/contents/piracki_kompas` into your server `plugins/ItemsAdder/contents/`.
2. Ensure textures are placed in `resourcepack/assets/piracki_kompas/textures/item/piracki_kompas/` with names `piracki_kompas_00.png` ... `piracki_kompas_31.png`.
	- If you don't have the textures yet, the model currently falls back to the vanilla compass texture so the item will appear in `/ia all`.
3. On the server run:

```
/iareload
/iazip
```

4. Watch console for ItemsAdder logs; the "Texture ... not found" error should disappear if texture filenames and paths match the model.
