def next_name(name):
    'Generate an infinite sequence of strings'
    if (name[-1] != 'z'):
        return name[:-1] + chr(ord(name[-1]) + 1)

    i = len(name) - 1
    while (i >= 0 and name[i] == 'z'):
        i -= 1
    first_z = i + 1

    count_z = len(name) - first_z
    if first_z >= 1:
        return name[:(first_z-1)] + chr(ord(name[first_z-1]) + 1) + 'a' * count_z
    return 'a' * (count_z + 1)

def generate_constants(n, prefix='c'):
    'Generate n constants'
    if (n == 0):
        return []
    s = ['a']
    for _ in range(n - 1):
        s.append(next_name(s[-1]))
    return [prefix + x for x in s]
