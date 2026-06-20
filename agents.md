# Boteco das IAs

## Infrastructure

This is a Java Spring Boot app that will use Spring AI and other necessary libs. 

Git Repository - https://github.com/boaglio/boteco-das-ias

## Purpose

The main purpose is to create a HTML info magazine to be sent to emails stored in a remote Google SpreadSheet . 

## Build Process 

This info magazine will dig in web for last week news related to these subjects: Java, Spring Boot, Spring AI, Technology, choose the best one of each subject and them separated it in a JSON file. 

And then each news will be sent to:
1-Claude (locally);
2-Ollama running GPT OSS model;
3-Ollama running LLama3 model;
4-Show me and prompt for my input. 

All opinions will be stored in this JSON file.

After gathering all opinions it will process one anime-like news-context-related image for each news. 

The final layout for the info magazine will be:

[title]
[image1] [news1]
[opinions1]
[news2]  [image2]
[opinions2]
[image3] [news3]
[opinions3]
[news4]  [image4]
[opinions4]
[footer]

This footer will contains social media info. 

## Mail Process

Mail will be a manual process to be executed by one shell script calling using GMail API .

## Storage Process

The final HTML should be stored in releases directory with its release date and the project README should be updated for quick access. 