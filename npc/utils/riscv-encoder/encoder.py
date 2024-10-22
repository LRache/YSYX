REGSITERS = {
    **{f"x{i}": i for i in range(32)},
    "ra": 1,
    "sp": 2,
    "gp": 3,
    "tp": 4,
    **{f"t{i}": i + 5 for i in range(3)}
}

def encode_register(name):
    pass