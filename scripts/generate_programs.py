import itertools
import multiprocessing
import os
import random
import subprocess
import time
import yaml

from generate_strings import generate_constants
from generate_facts import append_factsheets

CONFIG_FILE = '../config.yaml'
PROGRAMS_DIR = '../generated/programs/'
FULL_PROGRAMS_DIR = '../generated/full_programs/'
TIMEOUT = '60' # in seconds
FORBID_CYCLES = 'NEGATIVE'
REPEAT = 10

class Config:
    def __init__(self, max_num_nodes, max_num_clauses, num_predicates, max_arity, num_variables, num_constants,
                 num_independent_pairs, count, required_formula=None):
        self.forbidCycles = FORBID_CYCLES
        self.timeout = TIMEOUT + 's'
        self.prefix = '_'.join(str(x) for x in [num_predicates, num_variables, max_num_nodes, max_arity,
                                                num_independent_pairs, count])

        self.maxNumNodes = max_num_nodes
        self.maxNumClauses = max_num_clauses
        # making sure that max arity is achieved (the order doesn't matter anyway)
        self.arities = [max_arity] + [random.choice(range(1, max_arity + 1)) for _ in range(num_predicates - 1)]
        self.predicates = generate_constants(num_predicates, 'p')
        self.variables = [s.upper() for s in generate_constants(num_variables, 'v')]
        self.constants = generate_constants(num_constants, 'c')
        if required_formula:
            self.requiredFormula = {'operator': required_formula[0], 'predicates': required_formula[1]}

        self.independentPairs = []
        potential_pairs = list(itertools.combinations(self.predicates, 2))
        for p1, p2 in random.sample(potential_pairs, num_independent_pairs):
            self.independentPairs.append({'predicate1': p1, 'predicate2': p2})

    def save(self):
        stream = open(CONFIG_FILE, 'w')
        yaml.dump(self, stream)

def remove_programs(d):
    'Remove files from a directory'
    for filename in os.listdir(d):
        if filename.endswith('.pl'):
            os.remove(d + filename)

def generate_programs(args):
    num_predicates, num_variables, max_num_nodes, max_arity, num_independent_pairs = args
    for i in range(REPEAT):
        config = Config(max_num_nodes, num_predicates, num_predicates,
                        max_arity, num_variables, 0, num_independent_pairs, i)
        config.save()
        generator = subprocess.Popen(['mvn',  'exec:java', '-Dexec.mainClass=model.GeneratePrograms',
                                      '-Dexec.classpathScope=runtime', '-Dexec.args="normal"'], cwd="../")
        generator.wait()
        append_factsheets(config, i)

arguments = [(num_predicates, num_variables, max_num_nodes, max_arity, num_independent_pairs)
             for num_predicates, num_variables, max_num_nodes in itertools.product([2, 4, 8], repeat=3)
             for max_arity in [1, 2, 3]
             for num_independent_pairs in range(int(num_predicates * (num_predicates - 1) / 2) + 1)]
remove_programs(PROGRAMS_DIR)
remove_programs(FULL_PROGRAMS_DIR)
for args in arguments:
    generate_programs(args)

#run_problog()
#def run_problog():
#    'Run a set of experiments'
#    for filename in os.listdir(PROGRAMS_DIR):
#        sys.stdout.write('.')
#        sys.stdout.flush()
#        if filename.endswith('.pl'):
#            output = subprocess.Popen(['problog', '-v', PROGRAMS_DIR + filename, '-t', '60'],
#                                      stdout=subprocess.PIPE)
#            output_str = output.communicate()[0][:-1].decode('utf-8')
#            with open(OUTPUT_DIR + filename, 'w+') as f:
#                f.write(output_str)
