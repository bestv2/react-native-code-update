//
//  RCTUpdateManager.h
//  yimei_app
//
//  Created by 王舵 on 16/4/18.
//  Copyright © 2016年 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RCTBridgeModule.h"
#import "RCTEventEmitter.h"
#import "RCTFrameUpdate.h"
@protocol ReactNativeCodeUpdateDelegate <NSObject>

- (void)ReactNativeAutoUpdater_updateDownloadedToURL;
@end
@interface RCTUpdateManager : RCTEventEmitter <RCTBridgeModule>
//@property id<ReactNativeCodeUpdateDelegate> delegate;
+ (id)sharedInstance;
+ (NSURL *)binaryBundleURL;
/*
 * This method is used to retrieve the URL for the most recent
 * version of the JavaScript bundle. This could be either the
 * bundle that was packaged with the app binary, or the bundle
 * that was downloaded as part of a CodePush update. The value returned
 * should be used to "bootstrap" the React Native bridge.
 *
 * This method assumes that your JS bundle is named "main.jsbundle"
 * and therefore, if it isn't, you should use either the bundleURLForResource:
 * or bundleURLForResource:withExtension: methods to override that behavior.
 */
+ (NSURL *)bundleURL;

+ (NSURL *)bundleURLForResource:(NSString *)resourceName;

+ (NSURL *)bundleURLForResource:(NSString *)resourceName
                  withExtension:(NSString *)resourceExtension;

+ (NSString *)getApplicationSupportDirectory;
+ (id)checkUpdate;
+(void )doUpdate:(id)jsonObject doneCallBack:(RCTResponseSenderBlock) doneCallBack;
@end
