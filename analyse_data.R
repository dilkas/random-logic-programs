data <- read.csv("src/runtime.csv", header = FALSE, sep = ";", skip = 1)
names(data) <- c("numPredicates", "maxArity", "numVariables", "numConstants", "numAdditionalClauses", "numIndependentPairs", "maxNumNodes",
                 "solutionCount", "buildingTime", "totalTime", "initTime", "nodes", "backtracks", "fails", "restarts")

big <- data[data$totalTime == 60, ]

nrow(big)/nrow(data)*100
tail(sort(data$totalTime), 100)

boxplot(data$nodes ~ data$maxNumNodes)
boxplot(data$nodes ~ data$numIndependentPairs)
boxplot(data$nodes ~ data$maxArity)

quantile(data$totalTime, probs = 0.99)

hist(data$totalTime)
