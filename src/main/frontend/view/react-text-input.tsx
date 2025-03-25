import React, { useState } from 'react';
import { ReactAdapterElement, type RenderHooks } from 'Frontend/generated/flow/ReactAdapter';

class ReactTextInputElement extends ReactAdapterElement {
    constructor() {
        super();
    }

    protected override render(hooks: RenderHooks): React.ReactElement | null {
        const [value, setValue] = hooks.useState<string>('value');

        const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
            setValue(event.target.value);
            this.dispatchEvent(new CustomEvent('value-changed', { detail: { value: event.target.value } }));
        };

        return (
            <div>
                <input type="text" value={value} onChange={handleChange} />
                <span>{value?.length}     characters</span>
                
               
            </div>
        );
    }
}

customElements.define('react-text-input', ReactTextInputElement);