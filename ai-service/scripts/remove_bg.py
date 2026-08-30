"""
图片去背景工具
用法：python remove_bg.py <输入图片路径> [输出图片路径]
"""
import sys
from rembg import remove
from PIL import Image

input_path = sys.argv[1]
output_path = sys.argv[2] if len(sys.argv) > 2 else input_path.replace(".", "_nobg.")

with open(input_path, "rb") as f:
    input_data = f.read()

output_data = remove(input_data)

with open(output_path, "wb") as f:
    f.write(output_data)

print(f"✅ 去背景完成：{output_path}")