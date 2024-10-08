#ifndef __TEMP_DEPRESS_H__
#define __TEMP_DEPRESS_H__

#include <string>

class TempDecompressFile {
private:
    std::string tempFilename;
    bool failed = true;
public:
    TempDecompressFile(const std::string &filename);
    ~TempDecompressFile();
    bool is_failed() const;
    std::string get_temp_filename() const;
};

#endif