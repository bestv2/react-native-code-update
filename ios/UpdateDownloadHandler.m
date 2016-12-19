//
//  UpdateDownloadHandler.m
//  yimei_app
//
//  Created by 王舵 on 16/3/11.
//  Copyright © 2016年 Facebook. All rights reserved.
//

#import "UpdateDownloadHandler.h"

@implementation UpdateDownloadHandler{
	// Header chars used to determine if the file is a zip.
	char _header[4];
}
- (id)init:(NSString *)downloadFilePath
progressCallback:(void (^)(long long, long long))progressCallback
doneCallback:(void (^)(BOOL))doneCallback
failCallback:(void (^)(NSError *err))failCallback;
{
	self.savedFilePath = downloadFilePath;
	self.progressCallback = progressCallback;
	self.doneCallback = doneCallback;
	self.failCallback = failCallback;
	return self;
}
- (void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask didWriteData:(int64_t)bytesWritten totalBytesWritten:(int64_t)totalBytesWritten totalBytesExpectedToWrite:(int64_t)totalBytesExpectedToWrite{
	self.progressCallback(bytesWritten,(double)totalBytesWritten/totalBytesExpectedToWrite);
	NSLog(@"写入量:%lld 下载进度:%f",bytesWritten,(double)totalBytesWritten/totalBytesExpectedToWrite);
}
- (void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask didFinishDownloadingToURL:(NSURL *)location{
	//	NSString *path = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) lastObject];
	// 可以使用建议的文件名，与服务端一致
	//	NSString *file = [path stringByAppendingPathComponent:downloadTask.response.suggestedFilename];
	// 移动文件
		NSLog(@"下载结束啦");
	NSFileManager *mgr = [NSFileManager defaultManager];
		NSLog(@"文件是否存在：%d",[mgr fileExistsAtPath:location.path]);
	NSError *error = NULL;
	if([mgr fileExistsAtPath:self.savedFilePath]){
		[mgr removeItemAtPath:self.savedFilePath error:&error];
		if(error){
					self.failCallback(error);
					return;
		}

	}
	[mgr moveItemAtPath:location.path toPath:self.savedFilePath error:&error];
	if(error){
		self.failCallback(error);
		return;
	}
	//		NSLog(@"文件是否存在：%d",[mgr fileExistsAtPath:location.path]);
	
	self.doneCallback(true);
	
}
- (void)download:(NSString*)url{
	NSURLSession *session = [NSURLSession sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration] delegate:self delegateQueue:[NSOperationQueue mainQueue]];
	NSURLSessionDownloadTask *task = [session downloadTaskWithURL:[NSURL URLWithString:url] ];
//	NSLog(@"begin download");
	[task resume];
}
@end
