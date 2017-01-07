<?php

require "geshi.php";
$dir_name = "./geshi/";

$dir = opendir($dir_name);

while (($file = readdir($dir)) !== false) {
    echo "filename: $file : filetype: " . filetype($dir_name . $file) . "\n";
    if ($file !== "." && $file !== "..") {
        $path = "geshi/$file";
        require $path;
        $new_file_name = basename($path, ".php") . "_syntax.json";
        $json_contents = json_encode($language_data, JSON_PRETTY_PRINT);
        $fhandle = fopen($new_file_name, 'w') or die("Cannot create file: $new_file_name");
        fwrite($fhandle, $json_contents);
    }

}