from pysdd.sdd import SddManager, Vtree

var_count = 3
var_order = [1, 2, 3]
vtree_type = 'balanced'

vtree = Vtree(var_count, var_order, vtree_type)
manager = SddManager.from_vtree(vtree)

a, b, c = [manager.literal(i) for i in range(1, 4)]
alpha = c & (a | -b)

with open('sdd.dot', 'w') as out:
    print(alpha.dot(), file=out)
with open('vtree.dot', 'w') as out:
    print(vtree.dot(), file=out)
