data <- read.csv("src/runtime.csv", header = FALSE, sep = ";", skip = 1)
names(data) <- c("numPredicates", "maxArity", "numVariables", "numConstants", "numAdditionalClauses", "numIndependentPairs", "maxNumNodes",
                 "solutionCount", "buildingTime", "totalTime", "initTime", "nodes", "backtracks", "fails", "restarts")
data$percentageIndependent <- round(ifelse(data$numIndependentPairs >= 2, data$numIndependentPairs / choose(data$numPredicates, 2), 0), 2)

big <- data[data$totalTime == 60, ]
nrow(big)/nrow(data)*100
small <- data[data$totalTime < 1, ]
nrow(small)/nrow(data)*100

library(ggplot2)

boxplots <- function(x, y, xl = "", yl = "") {
  ggplot(data, aes(factor(x), y, group = x)) + geom_boxplot(outlier.shape = NA) +
    scale_y_continuous(limits = quantile(y, c(0.1, 0.9))) + xlab(xl) + ylab(yl)
}

boxplots(data$numVariables, data$totalTime)
boxplots(data$numVariables, data$nodes)
boxplots(data$numConstants, data$totalTime)
boxplots(data$numConstants, data$nodes)
boxplots(data$numAdditionalClauses, data$totalTime)
boxplots(data$numAdditionalClauses, data$nodes)
boxplots(data$maxNumNodes, data$totalTime)
boxplots(data$maxNumNodes, data$nodes)

boxplots(data$percentageIndependent, data$totalTime)
boxplots(data$percentageIndependent, data$nodes)

boxplots <- function(x, y) {
  ggplot(data, aes(factor(x), y, group = x)) + geom_boxplot(outlier.shape = NA) +
    scale_y_continuous(limits = quantile(y, c(0.1, 0.9)))
}
