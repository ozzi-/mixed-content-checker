![logo](https://i.imgur.com/sYVZyXi.png)

Do you have a website with [mixed content](https://developers.google.com/web/fundamentals/security/prevent-mixed-content/what-is-mixed-content) warnings? Don't want to manually search dozens of pages for the insecure content?
**MCC will crawl websites for you and tell you what you need to fix!**

MCC is currently supporting all causes of mixed content warnings:
* < img >
* < object >
* < iframe >
* < audio >
* < video >
* < script >
* < link >
* inline CSS URL attributes
* linked CSS URL attributes
* inline JS XHR calls
* linked JS XHR calls
* http links
* broken links

## Matching
MCC will crawl all found links on the page, if outgoing (links to another domain) links are defined, it will follow those. 
The -m (matches) parameter helps you to restrict / control the crawling behavior.
You can provide none, one or multiple (comma seperated) domains like such:

` -m domain.com,sub.domain.com,otherdomain.org `

All outgoing links **starting** with "domain.com", "sub.domain.com" and "otherdomain.org" will now be visited. "moredomain.com" won't - neither will "sub.otherdomain.org" or "www.domain.com". **Note:** https:// and http:// are stripped for the matching. 


## Screenshots
Usage:
![usage](https://i.imgur.com/Cw2TELj.png)

Example:
![example](https://i.imgur.com/6PD28EE.png)

