data <- read.csv("src/runtime.csv", header = FALSE, sep = ";", skip = 1)
names(data) <- c("numPredicates", "maxArity", "numVariables", "numConstants", "numAdditionalClauses", "numIndependentPairs", "maxNumNodes",
                 "solutionCount", "buildingTime", "initTime", "totalTime", "nodes", "backtracks", "fails", "restarts")
small <- data[sample(nrow(data), 1000), ]
boxplot(small$nodes ~ small$maxNumNodes)
