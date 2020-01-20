library(ggplot2)
library(tikzDevice)

df <- read.csv("src/runtime.csv", header = FALSE, sep = ";", skip = 1)
names(df) <- c("numPredicates", "maxArity", "numVariables", "numConstants", "numAdditionalClauses", "numIndependentPairs", "maxNumNodes",
                 "solutionCount", "buildingTime", "totalTime", "initTime", "nodes", "backtracks", "fails", "restarts")
df$percentageIndependent <- round(ifelse(df$numIndependentPairs >= 2, df$numIndependentPairs / choose(df$numPredicates, 2), 0), 2)

small <- df[df$totalTime < 1, ]
nrow(small)/nrow(df)*100
tail(sort(df$totalTime))

boxplots <- function(df, x, y) {
  ggplot(df, aes(factor(x), y, group = x)) +
    geom_boxplot(outlier.shape = NA) +
    coord_trans(y = "log10", limy = c(10, quantile(y, 0.92))) +
    scale_y_continuous(breaks = c(1, 10, 100, 1000, 10000),
                       labels = c("1", "10", expression(10^2), expression(10^3), expression(10^4)))
}

big <- df[df$numPredicates == 8, ]
tikz(file = "paper/phase_transition.tex", width = 6, height = 2)
ggplot(big, aes(factor(numIndependentPairs), nodes, group = numIndependentPairs)) +
  geom_boxplot(outlier.shape = NA) +
  coord_trans(y = "log10", limy = c(100, quantile(big$nodes, 0.91))) +
  scale_y_continuous(breaks = c(1, 20, 100, 1000, 10000),
                     labels = c("1", "20", expression(10^2), expression(10^3), expression(10^4))) +
  stat_summary(fun.y = mean, geom = "point") +
  xlab("Independent pairs") +
  ylab("Nodes")
dev.off()

# Positive correlation
cor.test(df$numVariables, df$totalTime)
cor.test(df$numVariables, df$nodes)
boxplots(df, df$numVariables, df$totalTime)
boxplots(df, df$numVariables, df$nodes)

cor.test(df$numConstants, df$totalTime)
cor.test(df$numConstants, df$nodes)
boxplots(df, df$numConstants, df$totalTime)
boxplots(df, df$numConstants, df$nodes)

cor.test(df$numAdditionalClauses, df$totalTime)
cor.test(df$numAdditionalClauses, df$nodes)
boxplots(df, df$numAdditionalClauses, df$totalTime)
boxplots(df, df$numAdditionalClauses, df$nodes)

boxplots(df, df$maxNumNodes, df$totalTime)
boxplots(df, df$maxNumNodes, df$nodes)

# A phase transition in mean but not median, i.e., some instances become really hard, but most remain easy


#               geom = "pointrange", color = "red")
#  stat_summary(fun.y = mean, geom = "point", shape = 20, size = 4, color = "red", fill = "red")

for (i in seq(1, 25)) {
  print(max(big[big$numIndependentPairs == i, "nodes"]))
  print(min(big[big$numIndependentPairs == i, "nodes"]))
}

interesting <- big[big$numIndependentPairs == 8, ]
hist(interesting[interesting$nodes < 1000, ]$nodes)
