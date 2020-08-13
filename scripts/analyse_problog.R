require(reshape2)
require(ggplot2)
require(gridExtra)
require(scales)
require(grid)
require(tikzDevice)

require(plyr)
require(tidyr)
require(tidyverse)
require(dplyr)

only_relevant_columns <- c("number.of.predicates", "number.of.variables",
                           "maximum.number.of.nodes", "maximum.arity",
                           "number.of.independent.pairs", "domain.size",
                           "number.of.facts", "proportion.probabilistic",
                           "proportion.listed", "proportion.independent.pairs", "universe.size", "instance")
relevant_columns <- c("number.of.predicates", "number.of.variables",
                      "maximum.number.of.nodes", "maximum.arity",
                      "number.of.independent.pairs", "domain.size",
                      "number.of.facts", "proportion.probabilistic",
                      "proportion.listed", "proportion.independent.pairs", "universe.size", "Total.time")
pretty_names <- c("Predicates", "Variables",
                  "Nodes", "Maximum arity",
                  "Independent", "Domain size",
                  "Facts ($\\times 10^3$)", "Probabilistic (\\%)",
                  "Proportion listed", "Independent (\\%)",
                  "Possible facts")

prepare.csv <- function(filename) {
  df <- read.csv(filename, header = TRUE, sep = ",")
  print(nrow(df))
  print(mean(is.na(df$answer)))
  df <- df[!is.na(df$answer),]
  df$proportion.listed <- df$number.of.facts / df$universe.size
  df$total.pairs <- df$number.of.predicates * (df$number.of.predicates - 1) / 2
  df$proportion.independent.pairs <- df$number.of.independent.pairs / df$total.pairs
  df %>% unite("ID", only_relevant_columns, remove = F)
}

# ==================== Data wrangling ========================

sdd <- prepare.csv("../data/problog/sdd.csv")
nnf <- prepare.csv("../data/problog/nnf.csv")
kbest <- prepare.csv("../data/problog/kbest.csv")
sddx <- prepare.csv("../data/problog/sddx.csv")
ddnnf <- prepare.csv("../data/problog/ddnnf.csv")
bdd <- prepare.csv("../data/problog/bdd.csv")

sdd <- sdd[sdd$ID %in% nnf$ID,]
sdd <- sdd[sdd$ID %in% kbest$ID,]
sdd <- sdd[sdd$ID %in% sddx$ID,]
sdd <- sdd[sdd$ID %in% ddnnf$ID,]
sdd <- sdd[sdd$ID %in% bdd$ID,]
nnf <- nnf[nnf$ID %in% sdd$ID,]
kbest <- kbest[kbest$ID %in% sdd$ID,]
sddx <- sddx[sddx$ID %in% sdd$ID,]
ddnnf <- ddnnf[ddnnf$ID %in% sdd$ID,]
bdd <- bdd[bdd$ID %in% sdd$ID,]

sdd <- sdd %>% mutate(count = map_int(Total.time, ~ sum(.x >= Total.time)))
nnf <- nnf %>% mutate(count = map_int(Total.time, ~ sum(.x >= Total.time)))
kbest <- kbest %>% mutate(count = map_int(Total.time, ~ sum(.x >= Total.time)))
sddx <- sddx %>% mutate(count = map_int(Total.time, ~ sum(.x >= Total.time)))
ddnnf <- ddnnf %>% mutate(count = map_int(Total.time, ~ sum(.x >= Total.time)))
bdd <- bdd %>% mutate(count = map_int(Total.time, ~ sum(.x >= Total.time)))

combined.pure <- rbind.fill(cbind(sdd, Algorithm = "SDD"),
                            cbind(nnf, Algorithm = "NNF"),
                            cbind(kbest, Algorithm = "K-Best"),
                            cbind(sddx, Algorithm = "SDDX"),
                            cbind(ddnnf, Algorithm = "d-DNNF"),
                            cbind(bdd, Algorithm = "BDD"))

combined <- rbind(cbind(melt(sdd[, relevant_columns], id.var = "Total.time"), Algorithm = "SDD"),
                  cbind(melt(nnf[, relevant_columns], id.var = "Total.time"), Algorithm = "NNF"),
                  cbind(melt(kbest[, relevant_columns], id.var = "Total.time"), Algorithm = "K-Best"),
                  cbind(melt(sddx[, relevant_columns], id.var = "Total.time"), Algorithm = "SDDX"),
                  cbind(melt(ddnnf[, relevant_columns], id.var = "Total.time"), Algorithm = "d-DNNF"),
                  cbind(melt(bdd[, relevant_columns], id.var = "Total.time"), Algorithm = "BDD"))

summarised <- combined %>% group_by(variable, value, Algorithm) %>%
  summarize(mean.time = mean(Total.time), sd.time = sd(Total.time))

# ====================== The Overview Plot =======================

get_legend <- function(myggplot){
  tmp <- ggplot_gtable(ggplot_build(myggplot))
  leg <- which(sapply(tmp$grobs, function(x) x$name) == "guide-box")
  legend <- tmp$grobs[[leg]]
  return(legend)
}

plot <- function(var.name, c, s, scale = F, dots = T, legend = F, smooth = F,
                 breaks = NULL, labels = NULL, ylims = NULL, separated = F, disable.y.axis = T, ribbon.size = 0.1) {
  if (smooth) {
    data <- c[c$variable == var.name,]
    y <- data$Total.time
  } else {
    data <- s[s$variable == var.name,]
    y <- data$mean.time
  }

  p <- ggplot(data = data, aes(x = value, y = y, color = Algorithm, fill = Algorithm, linetype = Algorithm)) +
    xlab(pretty_names[relevant_columns == var.name]) +
    ylab("Total time (s)") +
    theme_bw() +
    scale_colour_brewer(palette = "Dark2") +
    scale_fill_brewer(palette = "Dark2")

  if (disable.y.axis) {
    p <- p + theme(axis.title.y = element_blank())
  }
  if (smooth) {
    p <- p + geom_smooth(se = F)
  } else {
    p <- p + geom_line()
  }

  if (dots) {
    p <- p + geom_point()
  }
  if (scale && !is.null(breaks)) {
    p <- p + scale_x_continuous(breaks = breaks, labels = labels, trans = log10_trans())
  } else if (scale && is.null(breaks)) {
    p <- p + scale_x_continuous(trans = log2_trans())
  } else if (!scale && !is.null(breaks)) {
    p <- p + scale_x_continuous(breaks = breaks, labels = labels)
  } else if (separated) {
    p <- p + scale_x_continuous(labels = function(x) format(x, big.mark = " ", scientific = FALSE))
  }
  if (!legend) {
    p <- p + theme(legend.position = "none")
  } else {
    p <- p + guides(col = guide_legend(ncol = 2))
  }
  if (!is.null(ylims)) {
    p <- p + ylim(ylims[1], ylims[2])
  }
  p
}

# This might cause more questions than answer
tikz(file = "../paper/algorithms.tex", width = 4.8, height = 4.8, standAlone = T)
p1 <- plot("number.of.predicates", combined, summarised, legend = T)
legend <- get_legend(p1)
grid.arrange(plot("number.of.predicates", combined, summarised),
             plot("number.of.variables", combined, summarised),
             plot("maximum.number.of.nodes", combined, summarised),
             plot("maximum.arity", combined, summarised, breaks = c(1, 2, 3), labels = c(1, 2, 3)),
             plot("domain.size", combined, summarised),
             plot("proportion.probabilistic", combined, summarised, breaks = c(0.3, 0.4, 0.5, 0.6, 0.7), labels = c(30, 40, 50, 60, 70)),
             plot("number.of.facts", combined, summarised, separated = T, breaks = c(0, 25000, 50000, 75000, 100000), labels = c(0, 25, 50, 75, 100), dots = F),
             plot("number.of.independent.pairs", combined, summarised, dots = F),
             plot("proportion.independent.pairs", combined, summarised, dots = F, breaks = c(0, 0.25, 0.5, 0.75, 1), labels = c(0, 25, 50, 75, 100)),
             plot("proportion.listed", combined, summarised, F, F, smooth = T),
             plot("universe.size", combined, summarised, T, F, smooth = T, breaks = c(1e3, 1e4, 1e5, 1e6, 1e7, 1e8),
                  labels = c(expression(10^3), expression(10^4), expression(10^5),
                             expression(10^6), expression(10^7), expression(10^8))),
             legend,
             left = textGrob("Mean inference time (s)", rot = 90, vjust = 1))
dev.off()

# Extra plots for the talk

tikz(file = "../seminar_talk/proportion.tex", width = 4.2, height = 1.8, standAlone = T)
ggplot(data = combined[combined$variable == "proportion.listed",], aes(x = value, y = Total.time, color = Algorithm,
                                                                       fill = Algorithm, linetype = Algorithm)) +
  xlab("Proportion of listed facts") +
  ylab("Inference time (s)") +
  theme_bw() +
  scale_colour_brewer(palette = "Dark2") +
  scale_fill_brewer(palette = "Dark2") +
  geom_smooth(se = F)
dev.off()

plot("universe.size", combined, summarised, T, F, T, smooth = T, breaks = c(1e3, 1e4, 1e5, 1e6, 1e7, 1e8),
     labels = c(expression(10^3), expression(10^4), expression(10^5),
                expression(10^6), expression(10^7), expression(10^8)))

# ========================== Selecting a subset of data ======================

num_facts = 1e5
sdd2 <- sdd[sdd$number.of.facts == num_facts,]
nnf2 <- nnf[nnf$number.of.facts == num_facts,]
kbest2 <- kbest[kbest$number.of.facts == num_facts,]
sddx2 <- sddx[sddx$number.of.facts == num_facts,]
ddnnf2 <- ddnnf[ddnnf$number.of.facts == num_facts,]
bdd2 <- bdd[bdd$number.of.facts == num_facts,]

combined2 <- rbind(cbind(melt(sdd2[, relevant_columns], id.var = "Total.time"), Algorithm = "SDD"),
                  cbind(melt(nnf2[, relevant_columns], id.var = "Total.time"), Algorithm = "NNF"),
                  cbind(melt(kbest2[, relevant_columns], id.var = "Total.time"), Algorithm = "K-Best"),
                  cbind(melt(sddx2[, relevant_columns], id.var = "Total.time"), Algorithm = "SDDX"),
                  cbind(melt(ddnnf2[, relevant_columns], id.var = "Total.time"), Algorithm = "d-DNNF"),
                  cbind(melt(bdd2[, relevant_columns], id.var = "Total.time"), Algorithm = "BDD"))
summarised2 <- combined2 %>% group_by(variable, value, Algorithm) %>%
  summarize(mean.time = mean(Total.time), sd.time = sd(Total.time))
summary <- rbind(summarised2[summarised2$variable %in% c("proportion.independent.pairs", "Total.time"),],
                 summarised[summarised$variable %in% c("number.of.facts", "Total.time"),])


# ===================== Camera-ready ================================

plot2 <- function(df, column.names, df2 = NULL, column.names2 = NULL, factors = T, replace.labels = T) {
  smaller <- df[df$variable %in% column.names,]
  smaller$value <- as.factor(smaller$value)
  smaller$variable <- ifelse(smaller$variable == "maximum.arity", "Maximum arity", "Probabilistic")
  p <- ggplot(smaller, aes(x = value, y = Total.time, group = value))
  p <- p + geom_boxplot(outlier.shape = 4, outlier.alpha = 0.25)
  p + facet_wrap(~ variable, scales = "free_x") +
    scale_x_discrete() +
    ylab("Inference time (s)") +
    xlab("") +
    theme_bw()
}

tikz(file = "../paper/bars.tex", width = 2.4, height = 1.8)
plot2(combined2, c("maximum.arity", "proportion.probabilistic", "Total.time"))
dev.off()

# Paper version
summary$variable <- factor(summary$variable, levels = c("number.of.facts", "proportion.independent.pairs"),
                           labels = c("The number of facts ($\\times 10^3$)", "Proportion of independent pairs"))
tikz(file = "../paper/line_plots.tex", width = 4.8, height = 1.8)
ggplot(data = summary, aes(x = value, y = mean.time, color = Algorithm, shape = Algorithm)) +
  geom_line(aes(linetype = Algorithm)) +
  facet_wrap(~ variable, scales = "free_x") +
  xlab("") +
  ylab("Mean inference time (s)") +
  scale_x_continuous(labels = function(x) ifelse(x > 1e3, x / 1e3, x)) +
  scale_colour_brewer(palette = "Dark2") +
  theme_bw()
dev.off()

# Talk version
summary$variable <- factor(summary$variable, levels = c("number.of.facts", "proportion.independent.pairs"),
                           labels = c("Facts ($\\times 10^3$)", "Prop. independent"))
tikz(file = "../talk/line_plots.tex", width = 4.2, height = 1.8)
ggplot(data = summary, aes(x = value, y = mean.time, color = Algorithm, shape = Algorithm)) +
  geom_line(aes(linetype = Algorithm)) +
  facet_wrap(~ variable, scales = "free_x") +
  xlab("") +
  ylab("Mean inference time (s)") +
  scale_x_continuous(labels = function(x) ifelse(x > 1e3, x / 1e3, x)) +
  scale_colour_brewer(palette = "Dark2") +
  theme_bw()
dev.off()

merged <- merge(sdd, kbest, by = 'ID')
merged$proportion.independent.pairs.y <- merged$proportion.independent.pairs.y * 100
tikz(file = "../seminar_talk/scatterplot.tex", width = 4.8, height = 2.7, standAlone = T)
ggplot(merged, aes(x = Total.time.x, y = Total.time.y, color = proportion.independent.pairs.y)) +
  geom_point(alpha = 0.5, size = 1) +
  scale_x_continuous(limits = c(0.0164, 60), trans = log2_trans(), breaks = trans_breaks("log2", function(x) 2^x),
                     labels = trans_format("log2", math_format(2^.x))) +
  scale_y_continuous(limits = c(0.0164, 60), trans = log2_trans(), breaks = trans_breaks("log2", function(x) 2^x),
                     labels = trans_format("log2", math_format(2^.x))) +
  geom_abline(slope = 1, intercept = 0) +
  xlab("SDD inference time (s)") +
  ylab("SDDX inference time (s)") +
  theme_bw() +
  scale_colour_distiller(palette = "BuPu") +
  labs(color = "Independent (\\%)") +
  coord_fixed()
dev.off()

tikz(file = "../seminar_talk/cumulative.tex", width = 4.8, height = 1.8, standAlone = TRUE)
ggplot(data = combined.pure, aes(x = Total.time, y = count, color =  Algorithm, linetype = Algorithm)) +
  geom_line() +
  scale_x_continuous(trans = log2_trans(), breaks = c(0.0625, 0.25, 1, 4, 16), labels = c("0.0625", "0.25", "1", "4", "16")) +
  theme_bw() +
  xlab("Inference time (s)") +
  ylab("Instances solved") +
  scale_colour_brewer(palette = "Dark2")
dev.off()

# ================================ Misc ===========================================

summary(merged$Total.time.x - merged$Total.time.y)
different <- merged[merged$answer.x != merged$answer.y, ]
different2 <- different[, c("answer.x", "answer.y")]
differences <- as.numeric(different$answer.x) - as.numeric(different$answer.y)

cor(sdd[, -1])
model <- lm(Total.time ~ number.of.facts, data = sdd)
model <- lm(Total.time ~ number.of.facts + maximum.arity, data = sdd)
model <- lm(Total.time ~ number.of.facts + maximum.arity + proportion.probabilistic, data = sdd)
summary(model)
