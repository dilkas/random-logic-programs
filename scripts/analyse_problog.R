require(reshape2)
require(ggplot2)
require(dplyr)
require(gridExtra)
require(scales)
require(grid)
require(tidyr)

# TODO: reject all instances where at least one algorithm failed

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
  #df <- df[df$number.of.predicates == 8,]
  df$proportion.listed <- df$number.of.facts / df$universe.size
  df$total.pairs <- df$number.of.predicates * (df$number.of.predicates - 1) / 2
  df$proportion.independent.pairs <- df$number.of.independent.pairs / df$total.pairs
  df
}

sdd <- prepare.csv("../data/problog_sdd.csv")
nnf <- prepare.csv("../data/problog_nnf.csv")
symbolic <- prepare.csv("../data/problog_symbolic.csv")
melted.sdd <- melt(sdd[, relevant_columns], id.var = "Total.time")
melted.nnf <- melt(nnf[, relevant_columns], id.var = "Total.time")
melted.symbolic <- melt(symbolic[, relevant_columns], id.var = "Total.time")

combined <- rbind(cbind(melted.sdd, Algorithm = "sdd"), cbind(melted.nnf, Algorithm = "nnf"),
                  cbind(melted.symbolic, Algorithm = "symbolic"))
summarised <- combined %>% group_by(variable, value, Algorithm) %>%
  summarize(mean.time = mean(Total.time), sd.time = sd(Total.time))

get_legend <- function(myggplot){
  tmp <- ggplot_gtable(ggplot_build(myggplot))
  leg <- which(sapply(tmp$grobs, function(x) x$name) == "guide-box")
  legend <- tmp$grobs[[leg]]
  return(legend)
}

plot <- function(var.name, scale = F, dots = T, legend = F, smooth = F, breaks = NULL, labels = NULL, ylims = NULL) {
  if (smooth) {
    data <- combined[combined$variable == var.name,]
    y <- data$Total.time
  } else {
    data <- summarised[summarised$variable == var.name,]
    y <- data$mean.time
  }

  p <- ggplot(data = data,
              aes(x = value, y = y, color = Algorithm, fill = Algorithm, shape = Algorithm)) +
    xlab(pretty_names[relevant_columns == var.name]) +
    ylab("Total time") +
    theme_minimal() +
    theme(axis.title.y = element_blank()) +
    scale_colour_brewer(palette = "Set2") +
    scale_fill_brewer(palette = "Set2")

  if (smooth) {
    p <- p + geom_smooth()
  } else {
    p <- p + geom_line() +
      geom_ribbon(aes(ymin = mean.time - 0.1 * sd.time, ymax = mean.time + 0.1 * sd.time), alpha = 0.2, linetype = 0)
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
  }
  if (!legend) {
    p <- p + theme(legend.position = "none")
  }
  if (!is.null(ylims)) {
    p <- p + ylim(ylims[1], ylims[2])
  }
  p
}

# Grounding vs total time
ggplot(sdd, aes(x = Total.time, y = Grounding)) + geom_point()

p1 <- plot("number.of.predicates", legend = T)
legend <- get_legend(p1)
grid.arrange(plot("number.of.predicates"), plot("number.of.variables"),
             plot("maximum.number.of.nodes"), plot("maximum.arity", breaks = c(1, 2, 3), labels = c(1, 2, 3)),
             plot("domain.size"),
             plot("proportion.probabilistic"),
             plot("number.of.facts"),
             plot("number.of.independent.pairs"),
             plot("proportion.independent.pairs"),
             plot("proportion.listed", F, F, smooth = T),
             plot("universe.size", T, F, smooth = T, breaks = c(1e3, 1e4, 1e5, 1e6, 1e7, 1e8),
                  labels = c(expression(10^3), expression(10^4), expression(10^5),
                             expression(10^6), expression(10^7), expression(10^8))),
             legend,
             left = textGrob("Total time", rot = 90, vjust = 1))

united.sdd <- sdd %>% unite("ID", only_relevant_columns)
united.nnf <- nnf %>% unite("ID", only_relevant_columns)
united.symbolic <- symbolic %>% unite("ID", only_relevant_columns)
merged <- merge(united.sdd, united.symbolic, by = 'ID')

ggplot(merged, aes(x = Total.time.x, y = Total.time.y)) +
  geom_point() +
  scale_x_continuous(trans = log2_trans()) +
  scale_y_continuous(trans = log2_trans()) +
  geom_abline(colour = "blue")
