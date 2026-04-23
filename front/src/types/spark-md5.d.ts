// spark-md5.d.ts
declare module 'spark-md5' {
  class SparkMD5 {
    constructor();
    append(data: string | Uint8Array): SparkMD5;
    end(raw?: boolean): string;
    reset(): void;

    static ArrayBuffer(): ArrayBufferHelper;
  }

  interface ArrayBufferHelper {
    append(data: ArrayBuffer): ArrayBufferHelper;
    end(raw?: boolean): string;
    reset(): void;
  }

  export = SparkMD5;
}
