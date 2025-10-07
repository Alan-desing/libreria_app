from PIL import Image
img = Image.open("includes/img/logo.jpg").convert("RGBA")
img.save("app.ico", sizes=[(256,256),(128,128),(64,64),(48,48),(32,32),(16,16)])
print("Listo: app.ico")
