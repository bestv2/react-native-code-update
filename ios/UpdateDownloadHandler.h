//
//  UpdateDownloadHandler.h
//  yimei_app
//
//  Created by 王舵 on 16/3/11.
//  Copyright © 2016年 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface UpdateDownloadHandler : NSObject<NSURLSessionDownloadDelegate>
@property (strong) NSString *savedFilePath;
@property (copy) void (^progressCallback)(long long, long long);
@property (copy) void (^doneCallback)(BOOL);
@property (copy) void (^failCallback)(NSError *err);

- (id)init:(NSString *) downloadFilePaht
progressCallback:(void (^)(long long, long long))progressCallback
doneCallback:(void (^)(BOOL))doneCallback
failCallback:(void (^)(NSError *err))failCallback;

- (void)download:(NSString*)url;
@end
