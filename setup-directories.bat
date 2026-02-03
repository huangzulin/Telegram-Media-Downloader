@echo off
echo Creating required directories...

mkdir data 2>nul
mkdir downloads 2>nul
mkdir logs 2>nul
mkdir config 2>nul

echo Directory structure created:
dir