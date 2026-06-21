# Boteco das IAs

## Infrastructure

This is a Java Spring Boot app that will use Spring AI and other necessary libs. 

Git Repository - https://github.com/boaglio/boteco-das-ias

## Purpose

The main purpose is to create a HTML info magazine to be sent to emails stored in a remote Google SpreadSheet . 
The audience is mainly developers and IT students.
All text but technical terms must be translated to Brazilian Portuguese.   

## Build Process 

This info magazine will dig in web for last week news related to these subjects: Java, Spring Boot, Spring AI, Technology, choose the most popular news of each subject and them separated it in a JSON file. 

Do not repeat the same news for different subjects.

And then each news will be sent to:
1-Claude (locally);
2-Ollama running GPT OSS model;
3-Ollama running LLama3 model;
4-Show me and prompt for my input. 

All opinions will be stored in this JSON file.

After gathering all opinions it will process one anime-like news-context-related image for each news. 
These generated images MUST NOT HAVE any kind of text. 

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

Each opinion should be like a conversation (aligned on the left and right)
and have an icon/logo (small image from its maker).
This is the conversation sequence:
- my opinion (by input) 
- phi4-mini  opinion (by Ollama)
- LLama3.2 opinion (by Ollama)
- Claude opinion (by Claude Client)

This footer contains all links available in https://linktr.ee/boaglio (social media info and others). 

## Storage Process

The final HTML should be stored in releases directory with its release date and the project README should be updated for quick access.

## Output Process

This magazine should be available in HTML format as a release in GitHub page and in image format (one news + its conversation per image) to manually upload in a LinkedIn article.  
