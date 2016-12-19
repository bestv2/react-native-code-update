//
//  RCTUpdateManager.m
//  yimei_app
//
//  Created by 王舵 on 16/4/18.
//  Copyright © 2016年 Facebook. All rights reserved.
//

#import "RCTUpdateManager.h"
#import "RCTUpdateManager.h"
#import <UIKit/UIKit.h>
#import "UpdateDownloadHandler.h"
#import "SSZipArchive/SSZipArchive.h"

@implementation RCTUpdateManager
RCT_EXPORT_MODULE();
// These constants represent valid deployment statuses

// These keys represent the names we use to store data in NSUserDefaults

// These keys are already "namespaced" by the PendingUpdateKey, so
// their values don't need to be obfuscated to prevent collision with app data

// These keys are used to inspect/augment the metadata
// that is associated with an update's package.
static bool isFirstAccess = YES;
static RCTUpdateManager *RNAUTOUPDATER_SINGLETON = nil;

+ (id)sharedInstance
{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        isFirstAccess = NO;
        RNAUTOUPDATER_SINGLETON = [[super allocWithZone:NULL] init];
        [RNAUTOUPDATER_SINGLETON defaults];
    });

    return RNAUTOUPDATER_SINGLETON;
}

#pragma mark - Life Cycle

+ (id) allocWithZone:(NSZone *)zone {
    return [self sharedInstance];
}

+ (id)copyWithZone:(struct _NSZone *)zone {
    return [self sharedInstance];
}

+ (id)mutableCopyWithZone:(struct _NSZone *)zone {
    return [self sharedInstance];
}

- (id)copy {
    return [[RCTUpdateManager alloc] init];
}

- (id)mutableCopy {
    return [[RCTUpdateManager alloc] init];
}

- (id) init {
    if(RNAUTOUPDATER_SINGLETON){
        return RNAUTOUPDATER_SINGLETON;
    }
    if (isFirstAccess) {
        [self doesNotRecognizeSelector:_cmd];
    }
    self = [super init];
    return self;
}

- (void)defaults {
	
}

NSString * const UpdateFileName = @"update.zip";


#pragma mark - Static variables



// These values are used to save the bundleURL and extension for the JS bundle
// in the binary.
static NSString *JS_BUNDLE_VERSION_CODE = @"JS_BUNDLE_VERSION_CODE";
static NSString *JS_BUNDLE_PATH = @"JS_BUNDLE_PATH";
static NSString *UPDATED_APP_VERSION_CODE = @"UPDATED_APP_VERSION_CODE";



static NSString *bundleResourceExtension = @"jsbundle";
static NSString *bundleResourceName = @"main";
static NSString *bundleResourceFileName = @"main.jsbundle";

+ (NSURL *)binaryBundleURL
{
	return [[NSBundle mainBundle] URLForResource:bundleResourceName withExtension:bundleResourceExtension];
}

+ (NSURL *)bundleURL
{
	return [self bundleURLForResource:bundleResourceName];
}

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
{
	bundleResourceName = resourceName;
	return [self bundleURLForResource:resourceName
											withExtension:bundleResourceExtension];
}

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
									withExtension:(NSString *)resourceExtension
{
	bundleResourceName = resourceName;
	bundleResourceExtension = resourceExtension;
	//	NSError *error;
	NSURL *binaryBundleURL = [self binaryBundleURL];
	NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
	
	//	NSString *versionName =  [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleShortVersionString"];
	NSInteger versionCode =  [[[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"] intValue];
	NSInteger updatedAppVersionCode = [preferences integerForKey:UPDATED_APP_VERSION_CODE];
	NSLog(@"versionCode:%ld,updatedAppVersionCode:%ld",versionCode,updatedAppVersionCode);
	NSLog(@"JS_BUNDLE_PATH:%@",[preferences stringForKey:JS_BUNDLE_PATH]);
	if(updatedAppVersionCode != versionCode){
		[preferences setInteger:versionCode forKey:UPDATED_APP_VERSION_CODE];
		[preferences setObject:NULL forKey:JS_BUNDLE_PATH];
		[preferences setInteger:0 forKey:JS_BUNDLE_VERSION_CODE];
		//删除以前的文件
		NSFileManager *fileManager = [NSFileManager defaultManager];
		[fileManager removeItemAtPath:[self getBundleSavedPath] error:nil];
	}
 NSString *packageFile = [preferences objectForKey:JS_BUNDLE_PATH];
	
 
 
 if(packageFile != NULL){
	 
	 
		NSString *bundleFilePath = [[self getBundleSavedPath] stringByAppendingPathComponent:packageFile];
		
	 NSFileManager *fileManager = [NSFileManager defaultManager];
	 
	 if([fileManager fileExistsAtPath:bundleFilePath]){
			
		 NSURL *packageUrl = [[NSURL alloc] initFileURLWithPath:bundleFilePath];
			return packageUrl;
		 
	 }else {
		 [preferences setInteger:versionCode forKey:UPDATED_APP_VERSION_CODE];
		 [preferences setObject:NULL forKey:JS_BUNDLE_PATH];
		 [preferences setInteger:0 forKey:JS_BUNDLE_VERSION_CODE];
		 return binaryBundleURL;
	 }
	 
	 
 }else{
	 
	 NSLog(@"binaryBundleURL:%@",binaryBundleURL);
	 return binaryBundleURL;
	 
	}
	
}

+ (id) checkUpdate{
	NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
	NSInteger versionCode =  [[[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"] intValue];
	NSInteger jsVersionCode = (NSInteger)[preferences integerForKey:JS_BUNDLE_VERSION_CODE];
	//	NSString *temp = @"开始checkupdate 拉啊啦拉";
	NSString * const UPDATE_HOST = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"code.update.url"];
	NSString * const APP_ID = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"code.update.app_id"];
	NSString *urlString = [NSString stringWithFormat:@"%@/app/checkUpdate?platform=ios&app_id=%@&app_version_code=%ld&js_version_code=%ld",UPDATE_HOST,APP_ID,versionCode,jsVersionCode];
	NSLog(@"开始checkupdate 拉啊啦拉,urlString:%@",urlString);
	
	NSURL *url = [NSURL URLWithString:urlString];
	NSData *data = [NSData dataWithContentsOfURL:url];
	if(!data){
		return nil;
	}
	NSError *error = nil;
	
	id jsonObject = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:&error];
	if(error){
		NSLog(@"checkUpdate error:%@",error);
		return nil;
	}
	NSLog(@"%@",jsonObject);
	NSLog(@"%d",(int)[jsonObject integerForKey:@"success"]);
	
	NSLog(@"%@",[self getBundleSavedPath]);
	return jsonObject;
	//	return temp;
}
+(void) createDir:(NSString *) dirPath
{

}
+(void )doUpdate:(id)jsonObject doneCallBack:(RCTResponseSenderBlock) doneCallBack
{
	NSError *error = nil;
	if((int)[jsonObject integerForKey:@"success"]){
		if([jsonObject integerForKey:@"updateType"] == 2){
			NSLog(@"开始下载");
			NSFileManager *fileManager = [NSFileManager defaultManager];
			
			if(![fileManager fileExistsAtPath:[self getBundleSavedPath]]){
				[[NSFileManager defaultManager] createDirectoryAtPath:[self getBundleSavedPath]
																	withIntermediateDirectories:YES
																									 attributes:nil
																												error:&error];
				if(error){
					NSLog(@"error:%@",error);
					if(doneCallBack != nil){
						doneCallBack(@[@"false"]);
					}
					
					return;
				}
			}
			
			NSString *savedFilePath = [[self getUpdatePackageSavedPath] stringByAppendingPathComponent:UpdateFileName];
			if(![fileManager fileExistsAtPath:savedFilePath]){
				[[NSFileManager defaultManager] createDirectoryAtPath:savedFilePath
																	withIntermediateDirectories:YES
																									 attributes:nil
																												error:&error];
				if(error){
					NSLog(@"error:%@",error);
					if(doneCallBack != nil){
						doneCallBack(@[@"false"]);
					}
					
					return;
				}
			}
			
			UpdateDownloadHandler *downloadHandler =
			[[UpdateDownloadHandler alloc] init:savedFilePath
												 progressCallback:^(long long count, long long percetn){
														
												 }
														 doneCallback:^(BOOL success){
															 [fileManager removeItemAtPath:[self getBundleSavedPath] error:nil];
															 NSLog(@"回调结束啦");
															 
															 [SSZipArchive unzipFileAtPath:savedFilePath toDestination:[self getBundleSavedPath] delegate:self];
															 
															 NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
																[preferences setObject:bundleResourceFileName forKey:JS_BUNDLE_PATH];
																[preferences setInteger:[jsonObject integerForKey:@"jsBundleVersionCode"] forKey:JS_BUNDLE_VERSION_CODE];
															 
															 
															 NSString *filePathTemp = [self getBundleSavedPath];
															 
															 NSArray *fileList = [[NSArray alloc] init];
															 //fileList便是包含有该文件夹下所有文件的文件名及文件夹名的数组
															 NSError *fileError = NULL;
															 fileList = [fileManager contentsOfDirectoryAtPath:filePathTemp error:&fileError];
															 if(fileError){
																 NSLog(@"error:%@",error);
																 if(doneCallBack != nil){
																	 doneCallBack(@[@"false"]);
																 }
																 
																 return;
															 }
															 if(doneCallBack != nil){
																 doneCallBack(@[@"true"]);
															 }
															 
															 NSLog(@"路径==%@,fileList%@",filePathTemp,fileList);
															 //															 NSArray  *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
															 
															 
														 }
														 failCallback:^(NSError *err){
															 if(doneCallBack != nil){
																 doneCallBack(@[@"false"]);
															 }
															 
															 NSLog(@"error:%@",err);
														 }
			 ];
			[downloadHandler download:[jsonObject objectForKey:@"downloadUrl"]];
			return ;
		}else {
		NSLog(@"******************");
		if(doneCallBack != nil){
						doneCallBack(@[@"false"]);
					}
			return ;
		}
	}else {
		if(doneCallBack != nil){
						doneCallBack(@[@"false"]);
					}
	}
	return ;
}
+ (NSString *)getBundleSavedPath
{
	NSString* codePushPath = [[self getApplicationSupportDirectory] stringByAppendingPathComponent:@"js_bundle"];
	return codePushPath;
}
+ (NSString *)getUpdatePackageSavedPath
{
	NSString* codePushPath = [[self getApplicationSupportDirectory] stringByAppendingPathComponent:@"update_package"];
	return codePushPath;
}

+ (NSString *)getApplicationSupportDirectory
{
	NSString *applicationSupportDirectory = [NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES) objectAtIndex:0];
	return applicationSupportDirectory;
}
RCT_EXPORT_METHOD(checkUpdate:(RCTResponseSenderBlock)callback)
{
	id jsonObject = [RCTUpdateManager checkUpdate];
	NSLog(@"id%@",jsonObject);
	if(jsonObject == nil){
			callback(@[]);
	}else {
		callback(@[jsonObject]);
	}


}

RCT_EXPORT_METHOD(doUpdate:(id)jsonObject callback:(RCTResponseSenderBlock)callback)
{
	NSLog(@"%@",jsonObject);
	NSLog(@"doUpdate");
	[RCTUpdateManager doUpdate:jsonObject doneCallBack:callback];
//	[(AppDelegate*)[[UIApplication sharedApplication] delegate] ReactNativeAutoUpdater_updateDownloadedToURL];

}
RCT_EXPORT_METHOD(askForReload)
{
NSLog(@"askForReload,%@",self.delegate);
	if ([self.delegate respondsToSelector:@selector(ReactNativeAutoUpdater_updateDownloadedToURL)]) {
	NSLog(@"askForReload2");

	[self.delegate ReactNativeAutoUpdater_updateDownloadedToURL];
    }}
@end
