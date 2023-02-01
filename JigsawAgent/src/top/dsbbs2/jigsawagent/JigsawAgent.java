package top.dsbbs2.jigsawagent;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import io.github.karlatemp.unsafeaccessor.ModuleAccess;
import io.github.karlatemp.unsafeaccessor.Root;

public class JigsawAgent {
	@SuppressWarnings("unchecked")
	public static <T, R> R cast(T o) {
		return (R) o;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Throwable> void throwException(Throwable e) throws T {
		throw (T) e;
	}

	public static interface ThrowableConsumer<T> extends Consumer<T> {
		@Override
		default void accept(T o) {
			try {
				accept0(o);
			} catch (Throwable e) {
				throwException(e);
			}
		}

		void accept0(T o) throws Throwable;

		static <T> ThrowableConsumer<T> create(ThrowableConsumer<T> c) {
			return c;
		}

		static <T> ThrowableConsumer<T> fromConsumer(Consumer<T> c) {
			return c::accept;
		}
	}

	public static interface IgnoreThrowableConsumer<T> extends Consumer<T> {
		@Override
		default void accept(T o) {
			try {
				accept0(o);
			} catch (Throwable e) {
			}
		}

		void accept0(T o) throws Throwable;

		static <T> IgnoreThrowableConsumer<T> create(IgnoreThrowableConsumer<T> c) {
			return c;
		}

		static <T> IgnoreThrowableConsumer<T> fromConsumer(Consumer<T> c) {
			return c::accept;
		}
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public static void premain(String agentArgs, Instrumentation inst) throws Throwable {
		System.out.println("Hacking into Jigsaw...");
		Root.getUnsafe().ensureClassInitialized(Class.forName("sun.misc.Unsafe"));
		Lookup trusted = Root.getTrusted();
		Class<?> reflection = Class.forName("jdk.internal.reflect.Reflection");
		Object[] tmparr = new Object[] { null };
		trusted.findStaticSetter(reflection, "ALL_MEMBERS", java.util.Set.class).invokeWithArguments(tmparr);
		trusted.findStaticSetter(reflection, "WILDCARD", String.class).invokeWithArguments(tmparr);
		trusted.findStaticSetter(reflection, "methodFilterMap", java.util.Map.class).invokeWithArguments(tmparr);
		trusted.findStaticSetter(reflection, "fieldFilterMap", java.util.Map.class).invokeWithArguments(tmparr);
		ModuleAccess modacc = Root.getModuleAccess();
		Class<?> module = Class.forName("java.lang.Module");
		try {
			MethodHandle ena = trusted.findSetter(module, "enableNativeAccess", java.lang.Boolean.TYPE);
			tmparr = new Object[] { modacc.getALL_UNNAMED_MODULE(), true };
			try {
				ena.invokeWithArguments(tmparr);
			} catch (Throwable e) {
			}
			tmparr[0] = modacc.getEVERYONE_MODULE();
			try {
				ena.invokeWithArguments(tmparr);
			} catch (Throwable e) {
			}
		} catch (Throwable e) {
		}
		try {
			java.lang.reflect.Field ena = Root.openAccess(module.getDeclaredField("enableNativeAccess"));
			java.util.Collection<?> allunnmod = cast(
					Root.openAccess(module.getDeclaredField("ALL_UNNAMED_MODULE_SET")).get(null));
			allunnmod.parallelStream().forEach(IgnoreThrowableConsumer.create(i -> ena.set(i, true)));
			java.util.Collection<?> allmod = cast(Root.openAccess(module.getDeclaredField("EVERYONE_SET")).get(null));
			allmod.parallelStream().forEach(IgnoreThrowableConsumer.create(i -> ena.set(i, true)));
		} catch (Throwable e) {
			
		}
		try{
		Root.openAccess(Class.forName("jdk.internal.module.IllegalAccessLogger").getDeclaredField("logger")).set(null,
				null);
		}catch(Throwable t){}
		Method getMod = Class.class.getMethod("getModule");
		Method getPack = getMod.getReturnType().getMethod("getPackages");
		try (io.github.classgraph.ScanResult scrst = new io.github.classgraph.ClassGraph().enableClassInfo()
				.ignoreClassVisibility().enableInterClassDependencies().enableExternalClasses().scan()) {
			scrst.getAllClasses().loadClasses(true).parallelStream().forEach(ThrowableConsumer.create(i -> {
				((java.util.Collection<String>) (getPack.invoke(getMod.invoke(i)))).parallelStream().forEach(i2 -> {
					try {
						modacc.addOpens(getMod.invoke(i), i2, modacc.getEVERYONE_MODULE());
						/* System.out.println(i2); */
					} catch (Throwable e) {

					}
				});
			}));
		}
		reflection.getDeclaredField("fieldFilterMap").setAccessible(true);
		System.out.println("Successfully disable java9+ access controller!");
	}
}
