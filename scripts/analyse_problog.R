require(reshape2)
require(ggplot2)
require(dplyr)
require(gridExtra)
require(scales)
require(grid)
require(tidyr)
require(tikzDevice)

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
pretty_names <- c("The number of predicates", "The number of variables",
                  "Maximum number of nodes", "Maximum arity",
                  "The number of independent pairs", "Domain size",
                  "The number of facts", "The proportion of probabilistic facts",
                  "The proportion of listed facts", "The proportion of independent pairs",
                  "The number of possible facts")

prepare.csv <- function(filename) {
  df <- read.csv(filename, header = TRUE, sep = ",")
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
symbolic <- prepare.csv("../data/problog/symbolic.csv")
kbest <- prepare.csv("../data/problog/kbest.csv")
sddx <- prepare.csv("../data/problog/sddx.csv")
ddnnf <- prepare.csv("../data/problog/ddnnf.csv")

sdd <- sdd[sdd$ID %in% nnf$ID,]
sdd <- sdd[sdd$ID %in% symbolic$ID,]
sdd <- sdd[sdd$ID %in% kbest$ID,]
sdd <- sdd[sdd$ID %in% sddx$ID,]
sdd <- sdd[sdd$ID %in% ddnnf$ID,]
nnf <- nnf[nnf$ID %in% sdd$ID,]
symbolic <- symbolic[symbolic$ID %in% sdd$ID,]
kbest <- kbest[kbest$ID %in% sdd$ID,]
sddx <- sddx[sddx$ID %in% sdd$ID,]
ddnnf <- ddnnf[ddnnf$ID %in% sdd$ID,]

combined <- rbind(cbind(melt(sdd[, relevant_columns], id.var = "Total.time"), Algorithm = "SDD"),
                  cbind(melt(nnf[, relevant_columns], id.var = "Total.time"), Algorithm = "NNF"),
                  cbind(melt(symbolic[, relevant_columns], id.var = "Total.time"), Algorithm = "Symbolic"),
                  cbind(melt(kbest[, relevant_columns], id.var = "Total.time"), Algorithm = "K-Best"),
                  cbind(melt(sddx[, relevant_columns], id.var = "Total.time"), Algorithm = "SDDX"),
                  cbind(melt(ddnnf[, relevant_columns], id.var = "Total.time"), Algorithm = "dDNNF"))
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

  p <- ggplot(data = data,
              aes(x = value, y = y, color = Algorithm, fill = Algorithm, shape = Algorithm)) +
    xlab(pretty_names[relevant_columns == var.name]) +
    ylab("Total time (s)") +
    theme_minimal() +
    scale_colour_brewer(palette = "Set2") +
    scale_fill_brewer(palette = "Set2")

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
  }
  if (!is.null(ylims)) {
    p <- p + ylim(ylims[1], ylims[2])
  }
  p
}

p1 <- plot("number.of.predicates", combined, summarised, legend = T)
legend <- get_legend(p1)
grid.arrange(plot("number.of.predicates", combined, summarised),
             plot("number.of.variables", combined, summarised),
             plot("maximum.number.of.nodes", combined, summarised),
             plot("maximum.arity", combined, summarised, breaks = c(1, 2, 3), labels = c(1, 2, 3)),
             plot("domain.size", combined, summarised),
             plot("proportion.probabilistic", combined, summarised),
             plot("number.of.facts", combined, summarised, separated = T),
             plot("number.of.independent.pairs", combined, summarised),
             plot("proportion.independent.pairs", combined, summarised),
             plot("proportion.listed", combined, summarised, F, F, smooth = T),
             plot("universe.size", combined, summarised, T, F, smooth = T, breaks = c(1e3, 1e4, 1e5, 1e6, 1e7, 1e8),
                  labels = c(expression(10^3), expression(10^4), expression(10^5),
                             expression(10^6), expression(10^7), expression(10^8))),
             legend,
             left = textGrob("Total time (s)", rot = 90, vjust = 1))

# ======================= 1v1 comparison scatter plots ======================

merged <- merge(sdd, ddnnf, by = 'ID')
ggplot(merged, aes(x = Total.time.x, y = Total.time.y)) +
  geom_point() +
  scale_x_continuous(trans = log2_trans()) +
  scale_y_continuous(trans = log2_trans()) +
  geom_abline(colour = "blue")

# ========================== Selecting a subset of data ======================

num_facts = 1e5
sdd2 <- sdd[sdd$number.of.facts == num_facts,]
nnf2 <- nnf[nnf$number.of.facts == num_facts,]
symbolic2 <- symbolic[symbolic$number.of.facts == num_facts,]
kbest2 <- kbest[kbest$number.of.facts == num_facts,]
sddx2 <- sddx[sddx$number.of.facts == num_facts,]
ddnnf2 <- ddnnf[ddnnf$number.of.facts == num_facts,]

combined2 <- rbind(cbind(melt(sdd2[, relevant_columns], id.var = "Total.time"), Algorithm = "SDD"),
                  cbind(melt(nnf2[, relevant_columns], id.var = "Total.time"), Algorithm = "NNF"),
                  cbind(melt(symbolic2[, relevant_columns], id.var = "Total.time"), Algorithm = "Symbolic"),
                  cbind(melt(kbest2[, relevant_columns], id.var = "Total.time"), Algorithm = "K-Best"),
                  cbind(melt(sddx2[, relevant_columns], id.var = "Total.time"), Algorithm = "SDDX"),
                  cbind(melt(ddnnf2[, relevant_columns], id.var = "Total.time"), Algorithm = "dDNNF"))
summarised2 <- combined2 %>% group_by(variable, value, Algorithm) %>%
  summarize(mean.time = mean(Total.time), sd.time = sd(Total.time))
summary <- rbind(summarised2[summarised2$variable %in% c("proportion.independent.pairs", "Total.time"),],
                 summarised[summarised$variable %in% c("number.of.facts", "Total.time"),])
summary$variable <- factor(summary$variable, levels = c("number.of.facts", "proportion.independent.pairs"),
                           labels = c("The number of facts ($\\times 10^3$)", "Proportion of independent pairs"))

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

tikz(file = "../text/paper2/bars.tex", width = 2.4, height = 1.8)
plot2(combined2, c("maximum.arity", "proportion.probabilistic", "Total.time"))
dev.off()

tikz(file = "../text/paper2/line_plots.tex", width = 4.8, height = 1.8)
ggplot(data = summary, aes(x = value, y = mean.time, color = Algorithm, shape = Algorithm)) +
  geom_line(aes(linetype = Algorithm)) +
  facet_wrap(~ variable, scales = "free_x") +
  xlab("") +
  ylab("Mean inference time (s)") +
  scale_x_continuous(labels = function(x) ifelse(x > 1e3, x / 1e3, x)) +
  scale_colour_brewer(palette = "Dark2") +
  theme_bw()
dev.off()
