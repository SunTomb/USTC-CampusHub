from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / 'src/assets'
ROOT.mkdir(parents=True, exist_ok=True)
for name in ['tasks', 'shops', 'showcase', 'pass']:
    for active in [False, True]:
        im = Image.new('RGBA', (324, 324), (0, 0, 0, 0))
        draw = ImageDraw.Draw(im)
        color = '#16325B' if active else '#8492A6'
        width = 12
        if name == 'tasks':
            draw.line([(75, 240), (75, 150), (240, 150), (240, 75)], fill=color, width=width, joint='curve')
            draw.ellipse((43, 213, 107, 277), outline=color, width=width)
            draw.ellipse((208, 43, 272, 107), fill=color if active else None, outline=color, width=width)
            draw.line([(180, 105), (240, 150), (180, 195)], fill=color, width=width, joint='curve')
        elif name == 'shops':
            draw.rounded_rectangle((55, 125, 269, 276), radius=10, outline=color, width=width)
            draw.line([(40, 124), (68, 60), (256, 60), (284, 124)], fill=color, width=width, joint='curve')
            for x in [40, 101, 162, 223]:
                draw.arc((x, 98, x+61, 164), 0, 180, fill=color, width=width)
            draw.rounded_rectangle((121, 192, 203, 276), radius=5, outline=color, width=width)
        elif name == 'showcase':
            draw.rounded_rectangle((54, 47, 270, 277), radius=18, outline=color, width=width)
            draw.line([(95, 215), (229, 215)], fill=color, width=width)
            draw.line([(95, 246), (186, 246)], fill=color, width=width)
            draw.polygon([(162, 83), (180, 120), (220, 126), (191, 155), (198, 194), (162, 175), (126, 194), (133, 155), (104, 126), (144, 120)], fill=color if active else None, outline=color, width=9)
        else:
            draw.rounded_rectangle((39, 66, 285, 257), radius=22, outline=color, width=width)
            draw.ellipse((76, 108, 140, 172), fill=color if active else None, outline=color, width=width)
            draw.arc((65, 163, 151, 243), 180, 360, fill=color, width=width)
            draw.line([(178, 131), (248, 131)], fill=color, width=width)
            draw.line([(178, 176), (248, 176)], fill=color, width=width)
        im.resize((81, 81), Image.Resampling.LANCZOS).save(ROOT / ('tab-' + name + ('-active' if active else '') + '.png'), optimize=True)
print('Generated eight lightweight native tab icons.')
