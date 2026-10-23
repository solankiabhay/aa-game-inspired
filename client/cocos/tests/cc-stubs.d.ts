declare module "cc" {
  export class Component {
    node: Node;
    schedule(callback: (...args: any[]) => void, interval?: number, repeat?: number, delay?: number): void;
    scheduleOnce(callback: (...args: any[]) => void, delay?: number): void;
    unscheduleAllCallbacks(): void;
  }
  export class Node {
    active: boolean;
    getComponent<T>(type: new (...args: any[]) => T): T | null;
    getComponentInChildren<T>(type: new (...args: any[]) => T): T | null;
  }
  export class Label { string: string; }
  export class EditBox { string: string; }
  export class Button { interactable: boolean; node: Node; }
  export const _decorator: {
    ccclass(name: string): ClassDecorator;
    property(options?: any): PropertyDecorator;
    property(type?: any): PropertyDecorator;
  };
}
