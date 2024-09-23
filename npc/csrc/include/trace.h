namespace itrace
{
    void start(word_t pc);
    void trace(word_t pc);
    void end();
    void dump_to_file(const std::string &filename);
    void print();
} // namespace itrace