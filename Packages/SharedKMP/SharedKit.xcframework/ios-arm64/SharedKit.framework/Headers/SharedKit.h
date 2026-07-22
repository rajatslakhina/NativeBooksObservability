#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class SharedKitBook, SharedKitBooksRepository, SharedKitKmpSpanContext, SharedKitKmpSpanContextCompanion, SharedKitKmpSpanStatus, SharedKitKotlinArray<T>, SharedKitKotlinEnum<E>, SharedKitKotlinEnumCompanion, SharedKitKotlinException, SharedKitKotlinIllegalStateException, SharedKitKotlinRuntimeException, SharedKitKotlinThrowable, SharedKitLoginValidation, SharedKitNativeTracer;

@protocol SharedKitIosTracerProvider, SharedKitKmpTracer, SharedKitKotlinComparable, SharedKitKotlinIterator;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((swift_name("KotlinBase")))
@interface SharedKitBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface SharedKitBase (SharedKitBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface SharedKitMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface SharedKitMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorSharedKitKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface SharedKitNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface SharedKitByte : SharedKitNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface SharedKitUByte : SharedKitNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface SharedKitShort : SharedKitNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface SharedKitUShort : SharedKitNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface SharedKitInt : SharedKitNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface SharedKitUInt : SharedKitNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface SharedKitLong : SharedKitNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface SharedKitULong : SharedKitNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface SharedKitFloat : SharedKitNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface SharedKitDouble : SharedKitNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface SharedKitBoolean : SharedKitNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LoginValidation")))
@interface SharedKitLoginValidation : SharedKitBase
- (instancetype)initWithIsValid:(BOOL)isValid message:(NSString *)message __attribute__((swift_name("init(isValid:message:)"))) __attribute__((objc_designated_initializer));
- (SharedKitLoginValidation *)doCopyIsValid:(BOOL)isValid message:(NSString *)message __attribute__((swift_name("doCopy(isValid:message:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL isValid __attribute__((swift_name("isValid")));
@property (readonly) NSString *message __attribute__((swift_name("message")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LoginValidator")))
@interface SharedKitLoginValidator : SharedKitBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedKitLoginValidation *)validateEmail:(NSString *)email password:(NSString *)password __attribute__((swift_name("validate(email:password:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BooksRepository")))
@interface SharedKitBooksRepository : SharedKitBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedKitBook * _Nullable)bookId:(NSString *)id __attribute__((swift_name("book(id:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)loadBooksWithCompletionHandler:(void (^)(NSArray<SharedKitBook *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("loadBooks(completionHandler:)")));
@property (readonly) BOOL lastSpanEndAcknowledged __attribute__((swift_name("lastSpanEndAcknowledged")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Book")))
@interface SharedKitBook : SharedKitBase
- (instancetype)initWithId:(NSString *)id title:(NSString *)title author:(NSString *)author summary:(NSString *)summary category:(NSString *)category year:(int32_t)year readingMinutes:(int32_t)readingMinutes __attribute__((swift_name("init(id:title:author:summary:category:year:readingMinutes:)"))) __attribute__((objc_designated_initializer));
- (SharedKitBook *)doCopyId:(NSString *)id title:(NSString *)title author:(NSString *)author summary:(NSString *)summary category:(NSString *)category year:(int32_t)year readingMinutes:(int32_t)readingMinutes __attribute__((swift_name("doCopy(id:title:author:summary:category:year:readingMinutes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *author __attribute__((swift_name("author")));
@property (readonly) NSString *category __attribute__((swift_name("category")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) int32_t readingMinutes __attribute__((swift_name("readingMinutes")));
@property (readonly) NSString *summary __attribute__((swift_name("summary")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) int32_t year __attribute__((swift_name("year")));
@end


/** Implemented by Swift and backed by the native OpenTelemetry SDK. */
__attribute__((swift_name("IosTracerProvider")))
@protocol SharedKitIosTracerProvider
@required
- (BOOL)endNativeSpanContext:(SharedKitKmpSpanContext *)context attributes:(NSDictionary<NSString *, NSString *> *)attributes status:(SharedKitKmpSpanStatus *)status __attribute__((swift_name("endNativeSpan(context:attributes:status:)")));
- (SharedKitKmpSpanContext *)startNativeSpanName:(NSString *)name attributes:(NSDictionary<NSString *, NSString *> *)attributes __attribute__((swift_name("startNativeSpan(name:attributes:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KmpSpanContext")))
@interface SharedKitKmpSpanContext : SharedKitBase
- (instancetype)initWithTraceId:(NSString *)traceId spanId:(NSString *)spanId sampled:(BOOL)sampled __attribute__((swift_name("init(traceId:spanId:sampled:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKitKmpSpanContextCompanion *companion __attribute__((swift_name("companion")));
- (SharedKitKmpSpanContext *)doCopyTraceId:(NSString *)traceId spanId:(NSString *)spanId sampled:(BOOL)sampled __attribute__((swift_name("doCopy(traceId:spanId:sampled:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL isValid __attribute__((swift_name("isValid")));
@property (readonly) BOOL sampled __attribute__((swift_name("sampled")));
@property (readonly) NSString *spanId __attribute__((swift_name("spanId")));
@property (readonly) NSString *traceId __attribute__((swift_name("traceId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KmpSpanContext.Companion")))
@interface SharedKitKmpSpanContextCompanion : SharedKitBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKitKmpSpanContextCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedKitKmpSpanContext *NO_OP __attribute__((swift_name("NO_OP")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol SharedKitKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface SharedKitKotlinEnum<E> : SharedKitBase <SharedKitKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKitKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KmpSpanStatus")))
@interface SharedKitKmpSpanStatus : SharedKitKotlinEnum<SharedKitKmpSpanStatus *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKitKmpSpanStatus *unset __attribute__((swift_name("unset")));
@property (class, readonly) SharedKitKmpSpanStatus *ok __attribute__((swift_name("ok")));
@property (class, readonly) SharedKitKmpSpanStatus *error __attribute__((swift_name("error")));
+ (SharedKitKotlinArray<SharedKitKmpSpanStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKitKmpSpanStatus *> *entries __attribute__((swift_name("entries")));
@end


/** Platform-independent contract used by repositories and view models. */
__attribute__((swift_name("KmpTracer")))
@protocol SharedKitKmpTracer
@required

/** Returns true only when the native layer found and ended this exact span. */
- (BOOL)endSpanContext:(SharedKitKmpSpanContext *)context attributes:(NSDictionary<NSString *, NSString *> *)attributes status:(SharedKitKmpSpanStatus *)status __attribute__((swift_name("endSpan(context:attributes:status:)")));
- (SharedKitKmpSpanContext *)startSpanName:(NSString *)name attributes:(NSDictionary<NSString *, NSString *> *)attributes __attribute__((swift_name("startSpan(name:attributes:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NativeTracer")))
@interface SharedKitNativeTracer : SharedKitBase <SharedKitKmpTracer>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)nativeTracer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKitNativeTracer *shared __attribute__((swift_name("shared")));
- (BOOL)endSpanContext:(SharedKitKmpSpanContext *)context attributes:(NSDictionary<NSString *, NSString *> *)attributes status:(SharedKitKmpSpanStatus *)status __attribute__((swift_name("endSpan(context:attributes:status:)")));
- (void)initializeProvider:(id<SharedKitIosTracerProvider>)provider __attribute__((swift_name("initialize(provider:)")));
- (SharedKitKmpSpanContext *)startSpanName:(NSString *)name attributes:(NSDictionary<NSString *, NSString *> *)attributes __attribute__((swift_name("startSpan(name:attributes:)")));
@end


/** Business-facing view-model state; Swift only adapts it to ObservableObject. */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BooksViewModel")))
@interface SharedKitBooksViewModel : SharedKitBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithRepository:(SharedKitBooksRepository *)repository tracer:(id<SharedKitKmpTracer>)tracer __attribute__((swift_name("init(repository:tracer:)"))) __attribute__((objc_designated_initializer));
- (SharedKitBook * _Nullable)bookId:(NSString *)id __attribute__((swift_name("book(id:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)loadBooksForceReload:(BOOL)forceReload completionHandler:(void (^)(NSArray<SharedKitBook *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("loadBooks(forceReload:completionHandler:)")));
@property (readonly) NSArray<SharedKitBook *> *books __attribute__((swift_name("books")));
@property (readonly) NSString * _Nullable errorMessage __attribute__((swift_name("errorMessage")));
@property (readonly) BOOL isLoading __attribute__((swift_name("isLoading")));
@property (readonly) BOOL lastSpanEndAcknowledged __attribute__((swift_name("lastSpanEndAcknowledged")));
@end

__attribute__((swift_name("KotlinThrowable")))
@interface SharedKitKotlinThrowable : SharedKitBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKitKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKitKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
- (SharedKitKotlinArray<NSString *> *)getStackTrace __attribute__((swift_name("getStackTrace()")));
- (void)printStackTrace __attribute__((swift_name("printStackTrace()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKitKotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (NSError *)asError __attribute__((swift_name("asError()")));
@end

__attribute__((swift_name("KotlinException")))
@interface SharedKitKotlinException : SharedKitKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKitKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKitKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinRuntimeException")))
@interface SharedKitKotlinRuntimeException : SharedKitKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKitKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKitKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinIllegalStateException")))
@interface SharedKitKotlinIllegalStateException : SharedKitKotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKitKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKitKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
__attribute__((swift_name("KotlinCancellationException")))
@interface SharedKitKotlinCancellationException : SharedKitKotlinIllegalStateException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKitKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKitKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface SharedKitKotlinEnumCompanion : SharedKitBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKitKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface SharedKitKotlinArray<T> : SharedKitBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(SharedKitInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<SharedKitKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol SharedKitKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
