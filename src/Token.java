public enum Token {
    NOT {
        @Override
        public String toString() {
            return "\\+";
        }
    },

    AND {
        @Override
        public String toString() {
            return ",";
        }
    },

    OR {
        @Override
        public String toString() {
            return ";";
        }
    },

    TRUE {
        @Override
        public String toString() {
            return "";
        }
    }
}