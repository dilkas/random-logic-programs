library(ggplot2)
library(tikzDevice)
library(plyr)
library(dplyr)
library(ggrepel)
library(RColorBrewer)

# This is all for the runtime data of the CP model
df <- read.csv("../data/runtime.csv", header = FALSE, sep = ";", skip = 1)
df <- df[, colSums(is.na(df)) < nrow(df)]
names(df) <- c("numPredicates", "maxArity", "numVariables", "numConstants", "numAdditionalClauses", "numIndependentPairs", "maxNumNodes",
                 "solutionCount", "buildingTime", "totalTime", "initTime", "nodes", "backtracks", "fails", "restarts")
df$percentageIndependent <- round(ifelse(df$numIndependentPairs >= 2, df$numIndependentPairs / choose(df$numPredicates, 2), 0), 2)
df$numClauses <- df$numPredicates + df$numAdditionalClauses

# =========================================================

# Numbers
small <- df[df$totalTime < 1, ]
nrow(small)/nrow(df)*100
tail(sort(df$totalTime))
df %>% group_by(df$numPredicates) %>% tally()

# Phase transition: paper version
big <- df[df$numPredicates == 8, ]
tikz(file = "../paper/phase_transition.tex", width = 4.8, height = 1.8)
ggplot(big, aes(factor(numIndependentPairs), nodes, group = numIndependentPairs)) +
  geom_boxplot(outlier.shape = NA, fill = "#e0ecf4") +
  coord_trans(y = "log10", limy = c(100, quantile(big$nodes, 0.91))) +
  scale_y_continuous(breaks = c(1, 20, 100, 1000, 10000),
                     labels = c("1", "20", expression(10^2), expression(10^3), expression(10^4))) +
  stat_summary(fun.y = mean, geom = "point") +
  xlab("Independent pairs") +
  ylab("Nodes") +
  theme_set(theme_minimal(base_size = 7)) +
  theme_bw() +
  theme(legend.position = "none")
dev.off()

# Phase transition: talk version
tikz(file = "../seminar_talk/phase_transition.tex", width = 4.2, height = 1.8, standAlone = T)
ggplot(big, aes(factor(numIndependentPairs), nodes, group = numIndependentPairs)) +
  geom_boxplot(outlier.shape = NA, fill = "#e0ecf4") +
  coord_trans(y = "log10", limy = c(100, quantile(big$nodes, 0.91))) +
  scale_y_continuous(breaks = c(1, 20, 100, 1000, 10000),
                     labels = c("1", "20", expression(10^2), expression(10^3), expression(10^4))) +
  stat_summary(fun.y = mean, geom = "point") +
  xlab("Independent pairs of predicates") +
  ylab("Search nodes") +
  theme_set(theme_minimal(base_size = 7)) +
  theme_bw() +
  theme(legend.position = "none")
dev.off()

# Extra stuff to investigate the phase transition
bigs <- big %>% group_by(numIndependentPairs) %>% slice(which.max(nodes))
ggplot(bigs, aes(numIndependentPairs, nodes)) + geom_point()
ggplot(big, aes(factor(numIndependentPairs), totalTime, group = numIndependentPairs)) +
  geom_boxplot(outlier.shape = NA) +
  coord_trans(y = "log10") +
  scale_y_continuous(breaks = c(1, 20, 100, 1000, 10000),
                     labels = c("1", "20", expression(10^2), expression(10^3), expression(10^4))) +
  stat_summary(fun.y = mean, geom = "point") +
  xlab("Independent pairs") +
  ylab("Nodes")

# Looking for correlations

boxplots <- function(df, x, y) {
  ggplot(df, aes(factor(x), y, group = x)) +
    geom_boxplot(outlier.shape = NA) +
    coord_trans(y = "log10")
}

cor.test(df$numPredicates, df$nodes)
boxplots(df, df$numPredicates, df$nodes)

cor.test(df$maxArity, df$nodes)
boxplots(df, df$maxArity, df$nodes)

cor.test(df$numVariables, df$nodes)
boxplots(df, df$numVariables, df$nodes)

cor.test(df$numConstants, df$nodes)
boxplots(df, df$numConstants, df$nodes)

cor.test(df$numAdditionalClauses, df$nodes)
boxplots(df, df$numAdditionalClauses, df$nodes)

cor.test(df$maxNumNodes, df$nodes)
boxplots(df, df$maxNumNodes, df$nodes)

cor.test(df$numIndependentPairs, df$nodes)
boxplots(df, df$numIndependentPairs, df$nodes)
cor.test(df$percentageIndependent, df$nodes)
boxplots(df, df$percentageIndependent, df$nodes)

# ================================================================

# Each parameter is a colour
# 1, 2, 4, 8 on the x-axis
# Something that represents the distribution of nodes on the y-axis. Start with a mean or median.
# Pick mean and mention that the same plot for the median is similar.
# Mention log2 on the x-axis.

mytolatex <- function(name) {
  if (name == "maxNumNodes") {
    "$\\mathcal{M}_{\\mathcal{N}}$"
  } else if (name == "numAdditionalClauses") {
    "$\\mathcal{M}_{\\mathcal{C}}-|\\mathcal{P}|$"
  } else if (name == "numPredicates") {
    "$|\\mathcal{P}|$"
  } else if (name == "numVariables") {
    "$|\\mathcal{V}|$"
  } else if (name == "numConstants") {
    "$|\\mathcal{C}|$"
  } else {
    ""
  }
}

# reformat the table to have 3 columns: parameter_name, value (1, 2, 4, 8), and mean of nodes
effects <- data.frame(Variable = factor(), Value = integer(), mean = numeric(), aritylabel = character())
factors <- c('numPredicates', 'maxArity', 'numVariables', 'numConstants', 'numAdditionalClauses', 'maxNumNodes')
labels <- c("$|\\mathcal{P}|$", "$\\mathcal{M}_{\\mathcal{A}}$", "$|\\mathcal{V}|$", "$|\\mathcal{C}|$", "$\\mathcal{M}_{\\mathcal{C}}-|\\mathcal{P}|$", "$\\mathcal{M}_{\\mathcal{N}}$")
for (Variable in factors) {
  foo <- df %>% group_by_at(Variable) %>% summarise(mean(nodes)) %>% rename(Value = Variable, mean = "mean(nodes)")
#  foo$Variable <- factor(Variable, levels = factors, labels = labels)
  foo$Variable <- as.factor(Variable)
  foo$aritylabel <- ""
  effects <- rbind(effects, foo)
}
effects$label <- ifelse(effects$Value == 8, sapply(as.character(effects$Variable), mytolatex), "")
effects$aritylabel[effects$Variable == "maxArity" & effects$Value == 4] <- "$\\mathcal{M}_{\\mathcal{A}}$"

# Paper version
tikz(file = "../paper/impact.tex", width = 2.4, height = 1.8)
ggplot(effects, aes(Value, mean, color = factor(Variable))) +
  geom_line(aes(linetype = factor(Variable))) +
  scale_x_continuous(trans = "log2", limits = c(1, 10)) +
  ylab("Mean number of nodes") +
  geom_label_repel(aes(label = label, size = 1), nudge_x = 1, segment.color = "transparent", box.padding = 0.1) +
  geom_label_repel(aes(label = aritylabel, size = 1), segment.color = "transparent", nudge_y = 300) +
  xlab("The value of each parameter") +
  scale_color_brewer(palette = "Dark2") +
  scale_size_area(max_size = 2) +
  theme_bw() +
  theme(legend.position = "none")
dev.off()

# Talk version
effects$Variable <- revalue(effects$Variable, c("numPredicates" = "The number of predicates", "maxArity" = "Maximum arity",
                            "numVariables" = "The number of variables", "numConstants" = "The number of constants",
                            "numAdditionalClauses" = "The number of additional clauses", "maxNumNodes" = "The maximum number of nodes"))
tikz(file = "../talk/impact.tex", width = 4.2, height = 1.8)
ggplot(effects, aes(Value, mean, color = Variable)) +
  geom_line() +
  scale_x_continuous(trans = "log2", limits = c(1, 10)) +
  ylab("Search Nodes") +
  xlab("Value") +
  scale_color_brewer(palette = "Dark2") +
  scale_size_area(max_size = 2) +
  theme_bw()
dev.off()
