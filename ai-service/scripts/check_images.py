"""检查图片四角像素颜色，判断背景是否为纯色"""
from PIL import Image

for name in ['image1', 'image2', 'image3']:
    path = f'd:/study/project/campus-trade/{name}.png'
    img = Image.open(path)
    w, h = img.size
    print(f'{name}: size={img.size}, mode={img.mode}')
    corners = {
        'left-top': img.getpixel((0, 0)),
        'right-top': img.getpixel((w-1, 0)),
        'left-bottom': img.getpixel((0, h-1)),
        'right-bottom': img.getpixel((w-1, h-1)),
    }
    print(f'  corners: {corners}')
    center = img.getpixel((w//2, h//2))
    print(f'  center: {center}')
