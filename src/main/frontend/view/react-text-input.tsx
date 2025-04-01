// File: ReactTextInputElement.jsx
import React, { useEffect, useRef } from 'react';
import ReactDOM from 'react-dom/client';
import {
  DocumentEditorContainerComponent,
  Toolbar,
  Inject,
} from '@syncfusion/ej2-react-documenteditor';
import { registerLicense } from '@syncfusion/ej2-base';
import '/view/style.css';
registerLicense('Mgo+DSMBMAY9C3t2XVhhQlJHfVtdXGpWfFN0QHNYflR1dV9HZUwxOX1dQl9mSX1RdkdgXHtcdnBWQWk=');

// 1. Tạo React component
function ReactTextInput(props: { fileContent?: string, readOnly?: boolean }) {
  const containerRef = useRef<DocumentEditorContainerComponent>(null);

  // Đặt style container, bạn có thể điều chỉnh lại nếu cần
  const containerStyle = {
    width: props.readOnly ? 'auto' : '60%',
  };

  // Mở fileContent khi props thay đổi
  useEffect(() => {
    if (containerRef.current && props.fileContent) {
      (containerRef.current.documentEditor as any).open(props.fileContent, 'Docx');
      containerRef.current.documentEditor.isReadOnly = props.readOnly || false;
    }
  }, [props.fileContent, props.readOnly]);

  // Hàm lấy nội dung file (Base64) – dùng khi cần cập nhật nội dung
  const getUpdatedFileContent = (): Promise<string> => {
    return new Promise((resolve, reject) => {
      if (!containerRef.current) {
        return reject('Document Editor chưa sẵn sàng');
      }
      (containerRef.current.documentEditor as any)
        .saveAsBlob('Docx')
        .then((blob: Blob) => {
          const reader = new FileReader();
          reader.onloadend = () => {
            resolve(reader.result as string);
          };
          reader.onerror = (err) => reject(err);
          reader.readAsDataURL(blob);
        })
        .catch(reject);
    });
  };

  // Gán hàm vào global để Java có thể gọi qua executeJs nếu cần
  (window as any).getUpdatedFileContent = getUpdatedFileContent;

  return (
    <div style={containerStyle}>
      <DocumentEditorContainerComponent
        height="500"
        // Ẩn toolbar khi ở chế độ view (readOnly)
        enableToolbar={!props.readOnly}
        serviceUrl="https://services.syncfusion.com/react/production/api/documenteditor/"
        ref={containerRef}
      >
        { !props.readOnly && <Inject services={[Toolbar]} /> }
      </DocumentEditorContainerComponent>
    </div>
  );
}

// 2. Tạo custom element kế thừa HTMLElement
export class ReactTextInputElement extends HTMLElement {
  private root?: ReactDOM.Root;

  connectedCallback() {
    // Lấy thuộc tính fileContent và readOnly từ attribute (nếu có)
    const fileContentAttr = this.getAttribute('fileContent') || '';
    const readOnlyAttr = this.getAttribute('readOnly');
    const readOnly = readOnlyAttr !== null && readOnlyAttr.toLowerCase() === 'true';

    // Render React vào bên trong element này
    this.root = ReactDOM.createRoot(this);
    this.root.render(<ReactTextInput fileContent={fileContentAttr} readOnly={readOnly} />);
  }

  disconnectedCallback() {
    // Hủy React khi element bị remove (tùy chọn)
    this.root?.unmount();
  }
}

// 3. Đăng ký custom element
customElements.define('react-text-input', ReactTextInputElement);
