using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;

// The Content Dialog item template is documented at http://go.microsoft.com/fwlink/?LinkId=234238

namespace ImagePicker
{
    public sealed partial class OptionsModal : ContentDialog
    {
        public string Result { get; set; }

        public OptionsModal()
        {
            this.InitializeComponent();
            this.Result = "";

        }

        public void addButton(Button b)
        {
            b.Click += this.btnClick;
            this.buttonStack.Children.Add(b);
        }

        public void setTitle(string title)
        {
            this.dialog.Title = title;
        }

        // Handle the button clicks from dialog
        private void btnClick(object sender, RoutedEventArgs e)
        {
            this.Result = ((Button)sender).Tag.ToString();

            // Close the dialog
            dialog.Hide();
        }
    }
}
