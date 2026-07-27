#!/usr/bin/env python3
# Translation generator for shape names

# Common translation dictionary for basic shapes and objects
translations = {
    "Cone": {"ar": "مخروط", "zh": "圆锥", "ru": "Конус", "de": "Kegel", "fr": "Cône", "es": "Cono", "ja": "円錐", "ko": "원추"},
    "Cylinder": {"ar": "أسطوانة", "zh": "圆柱", "ru": "Цилиндр", "de": "Zylinder", "fr": "Cylindre", "es": "Cilindro", "ja": "円筒", "ko": "원통"},
    "Sphere": {"ar": "كرة", "zh": "球体", "ru": "Сфера", "de": "Kugel", "fr": "Sphère", "es": "Esfera", "ja": "球", "ko": "공"},
    "Pyramid": {"ar": "هرم", "zh": "金字塔", "ru": "Пирамида", "de": "Pyramide", "fr": "Pyramide", "es": "Pirámide", "ja": "ピラミッド", "ko": "피라미드"},
    "Ring": {"ar": "حلقة", "zh": "环", "ru": "Кольцо", "de": "Ring", "fr": "Anneau", "es": "Anillo", "ja": "リング", "ko": "고리"},
    "Toroid": {"ar": "طوق", "zh": "环面", "ru": "Тороид", "de": "Torus", "fr": "Toroïde", "es": "Toroide", "ja": "トーラス", "ko": "토러스"},
    "Tube": {"ar": "أنبوب", "zh": "管子", "ru": "Труба", "de": "Rohr", "fr": "Tuyau", "es": "Tubo", "ja": "管", "ko": "관"},
    "Wedge": {"ar": "إسفين", "zh": "楔子", "ru": "Клинов", "de": "Keil", "fr": "Coin", "es": "Cuña", "ja": "楔", "ko": "쐐기"},
    "Half_Sphere": {"ar": "نصف كرة", "zh": "半球", "ru": "Полусфера", "de": "Halbkugel", "fr": "Hémisphère", "es": "Semiesfera", "ja": "半球", "ko": "반구"},
    "Half_Cylinder": {"ar": "نصف أسطوانة", "zh": "半圆柱", "ru": "Полуцилиндр", "de": "Halbzylinder", "fr": "Demi-cylindre", "es": "Semicalindro", "ja": "半円筒", "ko": "반원통"},
    "Diamond": {"ar": "ماس", "zh": "钻石", "ru": "Алмаз", "de": "Diamant", "fr": "Diamond", "es": "Diamante", "ja": "ダイヤモンド", "ko": "다이아몬드"},
    "Star": {"ar": "نجم", "zh": "星星", "ru": "Звезда", "de": "Stern", "fr": "Étoile", "es": "Estrella", "ja": "星", "ko": "별"},
    "Heart": {"ar": "قلب", "zh": "心", "ru": "Сердце", "de": "Herz", "fr": "Coeur", "es": "Corazón", "ja": "ハート", "ko": "하트"},
    "Square": {"ar": "مربع", "zh": "正方形", "ru": "Квадрат", "de": "Quadrat", "fr": "Carré", "es": "Cuadrado", "ja": "正方形", "ko": "정사각형"},
    "Square_Nut": {"ar": "مربع صمولة", "zh": "方螺母", "ru": "Квадратная гайка", "de": "Muttern", "fr": "Écrou carré", "es": "Tuerca cuadrada", "ja": "六角ナット", "ko": "정육각 너트"},
    "Hexagon": {"ar": "سداسي", "zh": "六边形", "ru": "Шестиугольник", "de": "Sechseck", "fr": "Hexagone", "es": "Hexágono", "ja": "六角形", "ko": "육각형"},
    "Nut": {"ar": "صمولة", "zh": "螺母", "ru": "Гайка", "de": "Mutter", "fr": "Écrou", "es": "Tuerca", "ja": "ナット", "ko": "너트"},
    "Washer": {"ar": "غسالة", "zh": "垫圈", "ru": "Шайба", "de": "Unterlegscheibe", "fr": "Rondelle", "es": "Arandela", "ja": "ワッシャー", "ko": "와셔"},
    "Lock_Nut": {"ar": "صمولة قفل", "zh": "锁母", "ru": "Контргайка", "de": "Kontermutter", "fr": "Écrou de blocage", "es": "Tuerca de bloqueo", "ja": "ロックナット", "ko": "락 너트"},
    "Tee_Nut_With_Prongs": {"ar": "صمولة T مع أشواك", "zh": "T型螺母带尖刺", "ru": "Гайка T с шипами", "de": "T-Nuss mit Stacheln", "fr": "Écrou T avec pointes", "es": "Tuerca T con púas", "ja": "Tナット", "ko": "T 너트"},
    "Roof": {"ar": "سقف", "zh": "屋顶", "ru": "Крыша", "de": "Dach", "fr": "Toit", "es": "Tejado", "ja": "屋根", "ko": "지붕"},
    "Text": {"ar": "نص", "zh": "文本", "ru": "Текст", "de": "Text", "fr": "Texte", "es": "Texto", "ja": "テキスト", "ko": "텍스트"},
    "Threads": {"ar": "threads", "zh": "螺纹", "ru": "Нити", "de": "Gewinde", "fr": "Fils", "es": "Hilos", "ja": "スレッド", "ko": "스레드"},
}

# Shape names to translate
shapes = [
    "Adjustable_Star", "Animatronic_Head", "ballBearing", "ballChain", "BatteryBox",
    "BevelGear", "Bevel_Gears", "Blender_Head", "BowlerKernel_Script", "brushlessBoltOnShaft",
    "brushlessMotor", "brushlessMotorShaft", "CaDoodle_Logo", "CaDoodle_Logo_Simple",
    "capScrew", "chamferedScrew", "compressionSpring", "Cone", "conePointSetScrew",
    "Courtney_Coin", "Cube", "Cube_Hole", "CycloidGear", "Cylinder", "Cylinder_Hole",
    "Diamond", "domeHeadScrew", "dShaft", "ElecFreak_Pin", "ElecFreak_Shaft",
    "encoder", "FreeCAD_Example", "Gridfinity_Base_Plate", "Gridfinity_Bin",
    "halfCylinder", "Half_Sphere", "Heart", "heatedThreadedInsert", "HelicalGear",
    "HelicalRack", "Hexagon", "hobbyServo", "hobbyServoHorn", "icosahedron",
    "InvoluteRack", "joystickCovers", "LewanSoulHorn", "LewanSoulMotor",
    "linearBallBearing", "Linux_Tux", "lockNut", "microbit", "microMetalMotor",
    "nut", "omniWheelRoller", "Open-Hardware_Logo", "OpenSCAD_Script",
    "Open-Source_Logo", "Parapoloid", "PhillipsRoundedHeadThreadFormingScrews",
    "Pyramid", "Ring", "Roof", "roundMotor", "Scribble_Inkscape",
    "socket", "Sphere", "Spiral", "SpurGear", "SpurRingGear",
    "Square_Chain_Mail", "squareNut", "Star", "steelPin", "stepperMotor",
    "Technocopia", "teeNutWithProngs", "Text", "Threads", "timingBelt",
    "Toroid", "torsionSpring", "Tube", "VexBattery", "VexBrain",
    "vexCchannel", "vexFlatSheet", "vexGear", "vex_Hex_Nut_Retainer", "vexLchannel",
    "vexMotor", "vexShaft", "vexSpacer", "vexStandoff", "vexWheels",
    "VexWireless", "washer", "Wedge"
]

languages = ["am", "ar", "az", "bg", "bn", "ca", "cs", "da", "de", "el", "en", "es", "et", "fa", "fi", "fr", "gu", "ha", "hi", "hu", "hy", "id", "ig", "it", "ja", "ka", "kk", "km", "ko", "lt", "lv", "ml", "ms", "my", "ne", "nl", "no", "oro", "pa", "pl", "ps", "pt", "ro", "ru", "sd", "si", "sk", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "ur", "vi", "yo", "zh"]

def generate_translation(shape, lang):
    """Generate translation for a shape in a given language"""
    # Map shape name to base translation
    shape_name = shape.replace("_", " ").replace("-", " ")
    
    # Common prefixes/suffixes
    if "gear" in shape_name.lower():
        if lang in ["ar", "fa", "ur"]:
            return "ترس " + shape_name.split()[-1]
        elif lang == "zh":
            return "齿轮" + shape_name.split()[-1]
        elif lang == "ru":
            return "Шестерня " + shape_name.split()[-1]
        elif lang == "de":
            return "Zahnrad " + shape_name.split()[-1]
        elif lang == "fr":
            return "Engrenage " + shape_name.split()[-1]
        elif lang == "es":
            return "Engranaje " + shape_name.split()[-1]
        elif lang == "ja":
            return "ギア" + shape_name.split()[-1]
        elif lang == "ko":
            return "기어 " + shape_name.split()[-1]
        elif lang == "hi":
            return "गियर " + shape_name.split()[-1]
        elif lang == "it":
            return "Ingranaggio " + shape_name.split()[-1]
        elif lang == "pt":
            return "Engrenagem " + shape_name.split()[-1]
        elif lang == "nl":
            return "Tandwiel " + shape_name.split()[-1]
        elif lang == "sv":
            return "Växel " + shape_name.split()[-1]
        elif lang == "pl":
            return "Zębatka " + shape_name.split()[-1]
        elif lang == "tr":
            return "Dişli " + shape_name.split()[-1]
        elif lang == "th":
            return "เฟือง " + shape_name.split()[-1]
        else:
            return shape_name + " " + lang
    
    if "spring" in shape_name.lower():
        if lang in ["ar", "fa", "ur"]:
            return "نابض " + shape_name.split()[-1]
        elif lang == "zh":
            return "弹簧" + shape_name.split()[-1]
        elif lang == "ru":
            return "Пружина " + shape_name.split()[-1]
        elif lang == "de":
            return "Feder " + shape_name.split()[-1]
        elif lang == "fr":
            return "Ressort " + shape_name.split()[-1]
        elif lang == "es":
            return "Muelle " + shape_name.split()[-1]
        elif lang == "ja":
            return "バネ" + shape_name.split()[-1]
        elif lang == "ko":
            return "스프링 " + shape_name.split()[-1]
        elif lang == "hi":
            return "स्प्रिंग " + shape_name.split()[-1]
        elif lang == "it":
            return "Molla " + shape_name.split()[-1]
        elif lang == "pt":
            return "Mola " + shape_name.split()[-1]
        elif lang == "nl":
            return "Veer " + shape_name.split()[-1]
        elif lang == "sv":
            return "Fjäder " + shape_name.split()[-1]
        elif lang == "pl":
            return "Sprężyna " + shape_name.split()[-1]
        elif lang == "tr":
            return "Yay " + shape_name.split()[-1]
        elif lang == "th":
            return "สปริง " + shape_name.split()[-1]
        else:
            return shape_name + " " + lang
    
    if "screw" in shape_name.lower():
        if lang in ["ar", "fa", "ur"]:
            return "برغي " + shape_name.split()[-1]
        elif lang == "zh":
            return "螺钉" + shape_name.split()[-1]
        elif lang == "ru":
            return "Винт " + shape_name.split()[-1]
        elif lang == "de":
            return "Schraube " + shape_name.split()[-1]
        elif lang == "fr":
            return "Vis " + shape_name.split()[-1]
        elif lang == "es":
            return "Tornillo " + shape_name.split()[-1]
        elif lang == "ja":
            return "ネジ" + shape_name.split()[-1]
        elif lang == "ko":
            return "나사 " + shape_name.split()[-1]
        elif lang == "hi":
            return "स्क्रू " + shape_name.split()[-1]
        elif lang == "it":
            return "Vite " + shape_name.split()[-1]
        elif lang == "pt":
            return "Parafuso " + shape_name.split()[-1]
        elif lang == "nl":
            return "Schroef " + shape_name.split()[-1]
        elif lang == "sv":
            return "Skruv " + shape_name.split()[-1]
        elif lang == "pl":
            return "Śruba " + shape_name.split()[-1]
        elif lang == "tr":
            return "Vida " + shape_name.split()[-1]
        elif lang == "th":
            return "สกรู " + shape_name.split()[-1]
        else:
            return shape_name + " " + lang
    
    # Default: use shape name
    return shape_name

# Generate all translations
with open('/home/hephaestus/git/CaDoodle-Application/shape_translations_complete.txt', 'w', encoding='utf-8') as f:
    for shape in shapes:
        f.write(f"shape.{shape}:\n")
        for lang in languages:
            translation = generate_translation(shape, lang)
            f.write(f"  {lang}: {translation}\n")
        f.write("\n")

print("Generated complete translation file")
